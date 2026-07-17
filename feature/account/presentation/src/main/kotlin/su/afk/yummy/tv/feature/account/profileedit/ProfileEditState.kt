package su.afk.yummy.tv.feature.account.profileedit

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.account.model.LinkedAccountProvider
import su.afk.yummy.tv.domain.account.model.ProfileImageKind
import su.afk.yummy.tv.domain.account.model.ProfileListPrivacy
import su.afk.yummy.tv.domain.account.model.UserProfileSex

class ProfileEditState {
    data class State(
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val isImageLoading: Boolean = false,
        val hasLoadError: Boolean = false,
        val nickname: String = "",
        val avatarUrl: String? = null,
        val bannerUrl: String? = null,
        val pendingAvatarPreview: String? = null,
        val pendingBannerPreview: String? = null,
        val about: String = "",
        val birthDate: String = "",
        val sex: UserProfileSex = UserProfileSex.UNKNOWN,
        val listPrivacy: ProfileListPrivacy = ProfileListPrivacy.PUBLIC,
        val showShikimori: Boolean = true,
        val showTelegram: Boolean = true,
        val showVk: Boolean = true,
        val showDiscord: Boolean = true,
        val notifyTelegram: Boolean = false,
        val notifyVk: Boolean = false,
        val oldPassword: String = "",
        val newPassword: String = "",
        val confirmPassword: String = "",
        val isPasswordSaving: Boolean = false,
        val passwordValidationError: Boolean = false,
        val linkedAccounts: Set<LinkedAccountProvider> = emptySet(),
        val pendingUnlinkAccount: LinkedAccountProvider? = null,
        val unlinkingAccount: LinkedAccountProvider? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetrySelected : Event
        data class AboutChanged(val value: String) : Event
        data class BirthDateChanged(val value: String) : Event
        data class SexChanged(val value: UserProfileSex) : Event
        data class ListPrivacyChanged(val value: ProfileListPrivacy) : Event
        data class ShowShikimoriChanged(val value: Boolean) : Event
        data class ShowTelegramChanged(val value: Boolean) : Event
        data class ShowVkChanged(val value: Boolean) : Event
        data class ShowDiscordChanged(val value: Boolean) : Event
        data class NotifyTelegramChanged(val value: Boolean) : Event
        data class NotifyVkChanged(val value: Boolean) : Event
        data object SaveSelected : Event
        data class ImageSelected(
            val kind: ProfileImageKind,
            val bytes: ByteArray,
            val previewUri: String,
        ) : Event

        data class DeleteImageSelected(val kind: ProfileImageKind) : Event
        data class OldPasswordChanged(val value: String) : Event
        data class NewPasswordChanged(val value: String) : Event
        data class ConfirmPasswordChanged(val value: String) : Event
        data object ChangePasswordSelected : Event
        data class UnlinkAccountSelected(val provider: LinkedAccountProvider) : Event
        data object UnlinkAccountConfirmed : Event
        data object UnlinkAccountDismissed : Event
    }

    sealed interface Effect : UiEffect {
        data class Message(val type: MessageType) : Effect
    }

    enum class MessageType {
        PROFILE_SAVED,
        PROFILE_SAVE_FAILED,
        IMAGE_SAVED,
        IMAGE_SAVE_FAILED,
        PASSWORD_CHANGED,
        PASSWORD_CHANGE_FAILED,
        ACCOUNT_UNLINKED,
        ACCOUNT_UNLINK_FAILED,
    }
}
