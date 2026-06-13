package su.afk.yummy.tv.feature.account

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.feature.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.utils.loginCredentialsOrNull
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject internal constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val settingsStore: SettingsStore,
    private val yaniAuthPreferences: YaniAuthPreferences,
    private val detailsNavigator: IDetailsNavigator,
    private val authHandler: AccountAuthHandler,
    private val hubHandler: AccountHubHandler,
    private val notificationHandler: AccountNotificationHandler,
) : BaseViewModelNew<AccountState.State, AccountState.Event, AccountState.Effect>(savedStateHandle) {

    private var loadedUserId: Int = 0
    private var hasRefreshToken = false
    private var missingProfileRefreshAttempted = false
    private var isMissingProfileRefreshRunning = false

    override fun createInitialState() = AccountState.State()

    init {
        combine(
            yaniAuthPreferences.refreshToken,
            settingsStore.yaniUserId,
        ) { token, userId -> token to userId }
            .onEach { (token, userId) ->
                hasRefreshToken = token.isNotBlank()
                setState {
                    if (!hasRefreshToken) {
                        copy(
                            isSignedIn = false,
                            userId = 0,
                            stats = null,
                            notifications = emptyList(),
                            notificationCounts = emptyList(),
                            hubError = null,
                        )
                    } else {
                        copy(
                            isSignedIn = userId > 0,
                            userId = userId,
                        )
                    }
                }
                if (!hasRefreshToken) {
                    missingProfileRefreshAttempted = false
                    isMissingProfileRefreshRunning = false
                }
                recoverMissingProfileIfNeeded()
                maybeLoadHub()
            }
            .launchIn(viewModelScope)
        settingsStore.yaniNickname
            .onEach { setState { copy(nickname = it) } }
            .launchIn(viewModelScope)
        settingsStore.yaniAvatarUrl
            .onEach { setState { copy(avatarUrl = it) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: AccountState.Event) {
        when (event) {
            AccountState.Event.BackSelected -> {
                nav.back()
            }
            is AccountState.Event.TabSelected -> setState { copy(selectedTab = event.tab) }
            is AccountState.Event.LoginChanged -> setState {
                copy(
                    login = event.login,
                    error = null,
                    isCaptchaRequired = false,
                    captchaChallengeId = currentState.captchaChallengeId + 1,
                    captchaError = null,
                )
            }
            is AccountState.Event.PasswordChanged -> setState {
                copy(
                    password = event.password,
                    error = null,
                    isCaptchaRequired = false,
                    captchaChallengeId = currentState.captchaChallengeId + 1,
                    captchaError = null,
                )
            }
            AccountState.Event.LoginSelected -> {
                login()
            }
            is AccountState.Event.CaptchaSolved -> {
                if (event.token.isBlank()) {
                    setState { copy(captchaError = AccountUiError.CAPTCHA_RESPONSE_EMPTY) }
                } else {
                    login(captchaResponse = event.token)
                }
            }
            AccountState.Event.CaptchaExpired -> setState {
                copy(
                    isLoading = false,
                    captchaChallengeId = currentState.captchaChallengeId + 1,
                    captchaError = AccountUiError.CAPTCHA_EXPIRED,
                )
            }
            is AccountState.Event.CaptchaFailed -> setState {
                copy(
                    isLoading = false,
                    captchaChallengeId = currentState.captchaChallengeId + 1,
                    captchaError = AccountUiError.CAPTCHA_LOAD_FAILED,
                )
            }
            AccountState.Event.LogoutSelected -> viewModelScope.launch {
                setState { copy(isLoading = true, error = null) }
                if (authHandler.logout()) {
                    loadedUserId = 0
                    hasRefreshToken = false
                    missingProfileRefreshAttempted = false
                    isMissingProfileRefreshRunning = false
                    setState {
                        copy(
                            isLoading = false,
                            password = "",
                            selectedTab = AccountState.AccountTab.STATS,
                            stats = null,
                            notifications = emptyList(),
                            notificationCounts = emptyList(),
                            isCaptchaRequired = false,
                            captchaChallengeId = captchaChallengeId + 1,
                            captchaError = null,
                            hubError = null,
                        )
                    }
                    nav.back()
                } else {
                    setState {
                        copy(
                            isLoading = false,
                            error = AccountUiError.LOGOUT_FAILED
                        )
                    }
                }
            }
            AccountState.Event.RefreshProfileSelected -> viewModelScope.launch {
                setState { copy(isLoading = true, error = null) }
                when (authHandler.refreshProfile()) {
                    is AccountRefreshResult.Success -> {
                        setState { copy(isLoading = false) }
                        loadedUserId = 0
                        maybeLoadHub(force = true)
                    }

                    AccountRefreshResult.Failure -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = AccountUiError.REFRESH_FAILED
                            )
                        }
                    }
                }
            }
            AccountState.Event.RefreshHubSelected -> maybeLoadHub(force = true)
            is AccountState.Event.NotificationSelected -> openNotification(event.id)
            is AccountState.Event.NotificationFocused -> {
                if (currentState.focusedNotificationId != event.id) {
                    setState { copy(focusedNotificationId = event.id) }
                }
            }

            AccountState.Event.NotificationFocusRestoreHandled -> {
                if (currentState.restoreFocusedNotificationOnEnter) {
                    setState { copy(restoreFocusedNotificationOnEnter = false) }
                }
            }
            is AccountState.Event.NotificationReadSelected -> updateNotification(event.id) {
                notificationHandler.markNotificationRead(event.id)
            }
            AccountState.Event.AllNotificationsReadSelected -> markAllNotificationsReadOptimistically()
            is AccountState.Event.NotificationDeleteSelected -> updateNotification(event.id) {
                notificationHandler.deleteNotification(event.id)
            }
        }
    }

    private fun openNotification(id: Int) {
        val notification = currentState.notifications.firstOrNull { it.id == id } ?: return
        if (!notification.isNewEpisode) return
        val slug = notification.animeSlug ?: return
        viewModelScope.launch {
            setState {
                copy(
                    focusedNotificationId = id,
                    restoreFocusedNotificationOnEnter = true,
                    focusedNotificationRestoreToken = focusedNotificationRestoreToken + 1,
                    hubError = null,
                )
            }
            when (val result = notificationHandler.resolveAnimeId(slug)) {
                is AccountOpenNotificationResult.Navigate -> {
                    nav.navigate(detailsNavigator.getDetailsDest(result.animeId))
                }

                AccountOpenNotificationResult.Failure -> {
                    setState { copy(hubError = AccountUiError.OPEN_NOTIFICATION_FAILED) }
                }
            }
        }
    }

    private fun login(captchaResponse: String? = null) {
        val credentials = currentState.loginCredentialsOrNull()
        if (credentials == null) {
            setState {
                copy(
                    error = AccountUiError.CREDENTIALS_REQUIRED,
                    isCaptchaRequired = false,
                    captchaChallengeId = currentState.captchaChallengeId + 1,
                    captchaError = null,
                )
            }
            return
        }
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null, captchaError = null) }
            when (val result = authHandler.login(credentials, captchaResponse)) {
                is AccountLoginResult.Success -> {
                    loadedUserId = 0
                    missingProfileRefreshAttempted = false
                    isMissingProfileRefreshRunning = false
                    setState {
                        copy(
                            isLoading = false,
                            isSignedIn = result.account.id > 0,
                            password = "",
                            isCaptchaRequired = false,
                            captchaChallengeId = captchaChallengeId + 1,
                            captchaError = null,
                            userId = result.account.id,
                            nickname = result.account.nickname,
                            avatarUrl = result.account.avatarUrl.orEmpty(),
                        )
                    }
                    maybeLoadHub(force = true)
                }

                is AccountLoginResult.CaptchaRequired -> {
                    setState {
                        copy(
                            isLoading = false,
                            isCaptchaRequired = true,
                            captchaChallengeId = captchaChallengeId + 1,
                            captchaError = if (result.rejected) {
                                AccountUiError.CAPTCHA_REJECTED
                            } else {
                                null
                            },
                            error = null,
                        )
                    }
                }

                AccountLoginResult.Failure -> {
                    setState { copy(isLoading = false, error = AccountUiError.SIGN_IN_FAILED) }
                }
            }
        }
    }

    private fun recoverMissingProfileIfNeeded() {
        if (!hasRefreshToken) return
        if (currentState.userId > 0) return
        if (missingProfileRefreshAttempted || isMissingProfileRefreshRunning) return

        missingProfileRefreshAttempted = true
        isMissingProfileRefreshRunning = true
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            when (val result = authHandler.refreshProfile()) {
                is AccountRefreshResult.Success -> {
                    isMissingProfileRefreshRunning = false
                    if (result.account == null) {
                        setState { copy(isLoading = false, isSignedIn = false) }
                    } else {
                        loadedUserId = 0
                        setState {
                            copy(
                                isLoading = false,
                                isSignedIn = result.account.id > 0,
                                userId = result.account.id,
                                nickname = result.account.nickname,
                                avatarUrl = result.account.avatarUrl.orEmpty(),
                            )
                        }
                        maybeLoadHub(force = true)
                    }
                }

                AccountRefreshResult.Failure -> {
                    isMissingProfileRefreshRunning = false
                    setState {
                        copy(
                            isLoading = false,
                            isSignedIn = false,
                            error = AccountUiError.REFRESH_FAILED
                        )
                    }
                }
            }
        }
    }

    private fun maybeLoadHub(force: Boolean = false) {
        val state = currentState
        if (!state.isSignedIn || state.userId <= 0) return
        if (!force && loadedUserId == state.userId) return
        loadedUserId = state.userId
        viewModelScope.launch {
            setState { copy(isStatsLoading = true, isNotificationsLoading = true, hubError = null) }
            val result = hubHandler.loadHub(state.userId)
            setState {
                var next = copy(isStatsLoading = false)
                if (result.stats != null) {
                    next = next.copy(stats = result.stats)
                }
                if (result.statsError != null) {
                    next = next.copy(hubError = result.statsError)
                }
                next
            }
            applyNotificationsLoadResult(result.notifications)
        }
    }

    private suspend fun loadNotifications() {
        applyNotificationsLoadResult(hubHandler.loadNotifications())
    }

    private fun applyNotificationsLoadResult(result: AccountNotificationsLoadResult) {
        when (result) {
            is AccountNotificationsLoadResult.Success -> {
                setState {
                    copy(
                        notifications = result.notifications,
                        notificationCounts = result.counts,
                        isNotificationsLoading = false,
                    )
                }
            }

            is AccountNotificationsLoadResult.Failure -> {
                setState {
                    copy(
                        isNotificationsLoading = false,
                        hubError = result.error,
                    )
                }
            }
        }
    }

    private fun updateNotification(id: Int, action: suspend () -> Result<Boolean>) {
        viewModelScope.launch {
            setState { copy(isNotificationsLoading = true, hubError = null) }
            action().fold(
                onSuccess = {
                    if (it) {
                        loadNotifications()
                    } else {
                        setState { copy(isNotificationsLoading = false) }
                    }
                },
                onFailure = {
                    setState {
                        copy(
                            isNotificationsLoading = false,
                            hubError = AccountUiError.UPDATE_NOTIFICATION_FAILED,
                        )
                    }
                },
            )
        }
    }

    private fun markAllNotificationsReadOptimistically() {
        viewModelScope.launch {
            setState { copy(isNotificationsLoading = true, hubError = null) }
            notificationHandler.markAllNotificationsRead().fold(
                onSuccess = { updated ->
                    if (updated) {
                        loadNotifications()
                    } else {
                        setState { copy(isNotificationsLoading = false) }
                    }
                },
                onFailure = {
                    setState {
                        copy(
                            isNotificationsLoading = false,
                            hubError = AccountUiError.UPDATE_NOTIFICATIONS_FAILED,
                        )
                    }
                },
            )
        }
    }

}
