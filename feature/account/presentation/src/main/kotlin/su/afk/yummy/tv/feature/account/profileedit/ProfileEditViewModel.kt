package su.afk.yummy.tv.feature.account.profileedit

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.error.toDisplayDetails
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.domain.account.model.EditableProfile
import su.afk.yummy.tv.domain.account.model.ProfileUpdate
import su.afk.yummy.tv.feature.account.profileedit.handler.ProfileEditHandler
import su.afk.yummy.tv.feature.account.profileedit.mapper.toForm
import su.afk.yummy.tv.feature.account.profileedit.utils.toIsoDate
import javax.inject.Inject

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val handler: ProfileEditHandler,
) : BaseViewModel<ProfileEditState.State, ProfileEditState.Event, ProfileEditState.Effect>() {
    override fun createInitialState() = ProfileEditState.State()

    init {
        load()
    }

    override fun onEvent(event: ProfileEditState.Event) {
        when (event) {
            ProfileEditState.Event.BackSelected -> nav.back()
            ProfileEditState.Event.RetrySelected -> load()
            is ProfileEditState.Event.AboutChanged -> setState { copy(about = event.value) }
            is ProfileEditState.Event.BirthDateChanged -> setState { copy(birthDate = event.value) }
            is ProfileEditState.Event.SexChanged -> setState { copy(sex = event.value) }
            is ProfileEditState.Event.ListPrivacyChanged -> setState { copy(listPrivacy = event.value) }
            is ProfileEditState.Event.ShowShikimoriChanged -> setState { copy(showShikimori = event.value) }
            is ProfileEditState.Event.ShowTelegramChanged -> setState { copy(showTelegram = event.value) }
            is ProfileEditState.Event.ShowVkChanged -> setState { copy(showVk = event.value) }
            is ProfileEditState.Event.ShowDiscordChanged -> setState { copy(showDiscord = event.value) }
            is ProfileEditState.Event.NotifyTelegramChanged -> setState { copy(notifyTelegram = event.value) }
            is ProfileEditState.Event.NotifyVkChanged -> setState { copy(notifyVk = event.value) }
            ProfileEditState.Event.SaveSelected -> saveProfile()
            is ProfileEditState.Event.ImageSelected -> uploadImage(event)
            is ProfileEditState.Event.DeleteImageSelected -> deleteImage(event)
            is ProfileEditState.Event.OldPasswordChanged -> setState {
                copy(oldPassword = event.value, passwordValidationError = false)
            }

            is ProfileEditState.Event.NewPasswordChanged -> setState {
                copy(newPassword = event.value, passwordValidationError = false)
            }

            is ProfileEditState.Event.ConfirmPasswordChanged -> setState {
                copy(confirmPassword = event.value, passwordValidationError = false)
            }

            ProfileEditState.Event.ChangePasswordSelected -> changePassword()
            is ProfileEditState.Event.UnlinkAccountSelected -> {
                if (event.provider in currentState.linkedAccounts && currentState.unlinkingAccount == null) {
                    setState { copy(pendingUnlinkAccount = event.provider) }
                }
            }

            ProfileEditState.Event.UnlinkAccountDismissed -> {
                if (currentState.unlinkingAccount == null) setState { copy(pendingUnlinkAccount = null) }
            }

            ProfileEditState.Event.UnlinkAccountConfirmed -> unlinkAccount()
        }
    }

    private fun unlinkAccount() {
        val provider = currentState.pendingUnlinkAccount ?: return
        if (currentState.unlinkingAccount != null) return
        viewModelScope.launch {
            setState { copy(unlinkingAccount = provider) }
            runSuspendCatching { handler.unlinkAccount(provider) }
                .onSuccess {
                    applyLinkedAccounts(it)
                    setState { copy(pendingUnlinkAccount = null, unlinkingAccount = null) }
                    setEffect(ProfileEditState.Effect.Message(ProfileEditState.MessageType.ACCOUNT_UNLINKED))
                }
                .onFailure {
                    setState { copy(unlinkingAccount = null) }
                    setEffect(ProfileEditState.Effect.Message(ProfileEditState.MessageType.ACCOUNT_UNLINK_FAILED))
                }
        }
    }

    private fun load() = viewModelScope.launch {
        setState { copy(isLoading = true, hasLoadError = false, loadErrorDetails = null) }
        runSuspendCatching { handler.load() }
            .onSuccess(::applyProfile)
            .onFailure {
                val details = errorHandler.parse(it).toDisplayDetails()
                setState { copy(isLoading = false, hasLoadError = true, loadErrorDetails = details) }
            }
    }

    private fun saveProfile() = viewModelScope.launch {
        if (currentState.isSaving || !currentState.hasChanges) return@launch
        val state = currentState
        setState { copy(isSaving = true) }
        runSuspendCatching {
            handler.update(
                ProfileUpdate(
                    about = state.about.trim(),
                    birthDate = state.birthDate,
                    sex = state.sex,
                    listPrivacy = state.listPrivacy,
                    showShikimori = state.showShikimori,
                    showTelegram = state.showTelegram,
                    showVk = state.showVk,
                    showDiscord = state.showDiscord,
                    notifyTelegram = state.notifyTelegram,
                    notifyVk = state.notifyVk,
                )
            )
        }.onSuccess {
            applyForm(it)
            setEffect(ProfileEditState.Effect.Message(ProfileEditState.MessageType.PROFILE_SAVED))
        }.onFailure {
            setState { copy(isSaving = false) }
            setEffect(ProfileEditState.Effect.Message(ProfileEditState.MessageType.PROFILE_SAVE_FAILED))
        }
    }

    private fun uploadImage(event: ProfileEditState.Event.ImageSelected) = viewModelScope.launch {
        if (currentState.isImageLoading || event.bytes.isEmpty()) return@launch
        setState {
            copy(
                isImageLoading = true,
                pendingAvatarPreview = if (event.kind == su.afk.yummy.tv.domain.account.model.ProfileImageKind.AVATAR) event.previewUri else pendingAvatarPreview,
                pendingBannerPreview = if (event.kind == su.afk.yummy.tv.domain.account.model.ProfileImageKind.BANNER) event.previewUri else pendingBannerPreview,
            )
        }
        runSuspendCatching { handler.upload(event.kind, event.bytes) }
            .onSuccess {
                applyImages(it)
                setEffect(ProfileEditState.Effect.Message(ProfileEditState.MessageType.IMAGE_SAVED))
            }
            .onFailure {
                setState {
                    copy(
                        isImageLoading = false,
                        pendingAvatarPreview = null,
                        pendingBannerPreview = null,
                    )
                }
                setEffect(ProfileEditState.Effect.Message(ProfileEditState.MessageType.IMAGE_SAVE_FAILED))
            }
    }

    private fun deleteImage(event: ProfileEditState.Event.DeleteImageSelected) =
        viewModelScope.launch {
            if (currentState.isImageLoading) return@launch
            setState { copy(isImageLoading = true) }
            runSuspendCatching { handler.delete(event.kind) }
                .onSuccess {
                    applyImages(it)
                    setEffect(ProfileEditState.Effect.Message(ProfileEditState.MessageType.IMAGE_SAVED))
                }
                .onFailure {
                    setState { copy(isImageLoading = false) }
                    setEffect(ProfileEditState.Effect.Message(ProfileEditState.MessageType.IMAGE_SAVE_FAILED))
                }
        }

    private fun changePassword() = viewModelScope.launch {
        val state = currentState
        if (state.isPasswordSaving) return@launch
        if (state.oldPassword.isBlank() || state.newPassword.isBlank() ||
            state.newPassword != state.confirmPassword
        ) {
            setState { copy(passwordValidationError = true) }
            return@launch
        }
        setState { copy(isPasswordSaving = true, passwordValidationError = false) }
        runSuspendCatching { handler.changePassword(state.oldPassword, state.newPassword) }
            .onSuccess {
                setState {
                    copy(
                        oldPassword = "",
                        newPassword = "",
                        confirmPassword = "",
                        isPasswordSaving = false,
                    )
                }
                setEffect(ProfileEditState.Effect.Message(ProfileEditState.MessageType.PASSWORD_CHANGED))
            }
            .onFailure {
                setState { copy(isPasswordSaving = false) }
                setEffect(ProfileEditState.Effect.Message(ProfileEditState.MessageType.PASSWORD_CHANGE_FAILED))
            }
    }

    /** Первичная загрузка: применяем профиль целиком. */
    private fun applyProfile(profile: EditableProfile) {
        applyForm(profile)
        applyImages(profile)
        applyLinkedAccounts(profile)
    }

    /**
     * Поля формы и флаги загрузки/сохранения. Не трогает картинки и привязки, чтобы сохранение,
     * пересёкшееся с загрузкой аватара или отвязкой, не сбрасывало чужой прогресс.
     */
    private fun applyForm(profile: EditableProfile) {
        setState {
            copy(
                isLoading = false,
                isSaving = false,
                hasLoadError = false,
                nickname = profile.nickname,
                about = profile.about,
                birthDate = profile.birthDateSeconds.toIsoDate(),
                sex = profile.sex,
                listPrivacy = profile.listPrivacy,
                showShikimori = profile.showShikimori,
                showTelegram = profile.showTelegram,
                showVk = profile.showVk,
                showDiscord = profile.showDiscord,
                notifyTelegram = profile.notifyTelegram,
                notifyVk = profile.notifyVk,
            ).let { it.copy(savedForm = it.toForm()) }
        }
    }

    /** Аватар и баннер: несохранённые правки формы остаются как есть. */
    private fun applyImages(profile: EditableProfile) {
        setState {
            copy(
                isImageLoading = false,
                avatarUrl = profile.avatarUrl,
                bannerUrl = profile.bannerUrl,
                pendingAvatarPreview = null,
                pendingBannerPreview = null,
            )
        }
    }

    private fun applyLinkedAccounts(profile: EditableProfile) {
        setState { copy(linkedAccounts = profile.linkedAccounts.toImmutableSet()) }
    }

}
