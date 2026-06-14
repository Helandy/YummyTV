package su.afk.yummy.tv.feature.account

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.analytics.AnalyticsEvents
import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.feature.account.handler.AccountHubHandler
import su.afk.yummy.tv.feature.account.handler.AccountLoginResult
import su.afk.yummy.tv.feature.account.handler.AccountNotificationHandler
import su.afk.yummy.tv.feature.account.handler.AccountNotificationMutationHandler
import su.afk.yummy.tv.feature.account.handler.AccountNotificationMutationResult
import su.afk.yummy.tv.feature.account.handler.AccountNotificationsLoadResult
import su.afk.yummy.tv.feature.account.handler.AccountOpenNotificationResult
import su.afk.yummy.tv.feature.account.handler.AccountRefreshResult
import su.afk.yummy.tv.feature.account.handler.AccountSessionHandler
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
    private val sessionHandler: AccountSessionHandler,
    private val hubHandler: AccountHubHandler,
    private val notificationHandler: AccountNotificationHandler,
    private val notificationMutationHandler: AccountNotificationMutationHandler,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModelNew<AccountState.State, AccountState.Event, AccountState.Effect>(savedStateHandle) {

    override fun createInitialState() = AccountState.State()

    init {
        combine(
            yaniAuthPreferences.refreshToken,
            settingsStore.yaniUserId,
        ) { token, userId -> token to userId }
            .onEach { (token, userId) ->
                sessionHandler.onAuthSnapshot(token)
                val hasRefreshToken = sessionHandler.hasRefreshToken()
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

            is AccountState.Event.TabSelected -> {
                if (event.tab != currentState.selectedTab) {
                    trackAccountAction(
                        action = "tab_selected",
                        params = analyticsParamsOf("tab" to event.tab.name.lowercase()),
                    )
                }
                setState { copy(selectedTab = event.tab) }
            }

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
                trackAccountAction("login_selected")
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
                trackAccountAction("logout_selected")
                setState { copy(isLoading = true, error = null) }
                if (sessionHandler.logout()) {
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
                trackAccountAction("refresh_profile_selected")
                setState { copy(isLoading = true, error = null) }
                when (sessionHandler.refreshProfile()) {
                    is AccountRefreshResult.Success -> {
                        setState { copy(isLoading = false) }
                        sessionHandler.markProfileChanged()
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

            AccountState.Event.RefreshHubSelected -> {
                trackAccountAction("refresh_hub_selected")
                maybeLoadHub(force = true)
            }

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

            is AccountState.Event.NotificationReadSelected -> {
                trackAccountAction(
                    action = "notification_read_selected",
                    params = analyticsParamsOf("notification_id" to event.id),
                )
                updateNotification {
                    notificationMutationHandler.markNotificationRead(event.id)
                }
            }

            AccountState.Event.AllNotificationsReadSelected -> {
                trackAccountAction("all_notifications_read_selected")
                updateNotification {
                    notificationMutationHandler.markAllNotificationsRead()
                }
            }

            is AccountState.Event.NotificationDeleteSelected -> {
                trackAccountAction(
                    action = "notification_delete_selected",
                    params = analyticsParamsOf("notification_id" to event.id),
                )
                updateNotification {
                    notificationMutationHandler.deleteNotification(event.id)
                }
            }
        }
    }

    private fun openNotification(id: Int) {
        val notification = currentState.notifications.firstOrNull { it.id == id } ?: return
        if (!notification.isNewEpisode) return
        val slug = notification.animeSlug ?: return
        trackAccountAction(
            action = "notification_selected",
            params = analyticsParamsOf("notification_id" to id),
        )
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
            when (val result = sessionHandler.login(credentials, captchaResponse)) {
                is AccountLoginResult.Success -> {
                    sessionHandler.markProfileChanged()
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
        if (!sessionHandler.beginMissingProfileRecoveryIfNeeded(currentState)) return
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            when (val result = sessionHandler.refreshProfile()) {
                is AccountRefreshResult.Success -> {
                    sessionHandler.completeMissingProfileRecovery()
                    if (result.account == null) {
                        setState { copy(isLoading = false, isSignedIn = false) }
                    } else {
                        sessionHandler.markProfileChanged()
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
                    sessionHandler.completeMissingProfileRecovery()
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
        if (!sessionHandler.markHubLoadIfNeeded(state, force)) return
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

    private fun updateNotification(action: suspend () -> AccountNotificationMutationResult) {
        viewModelScope.launch {
            setState { copy(isNotificationsLoading = true, hubError = null) }
            when (val result = action()) {
                AccountNotificationMutationResult.Unchanged -> {
                    setState { copy(isNotificationsLoading = false) }
                }

                is AccountNotificationMutationResult.Reloaded -> {
                    applyNotificationsLoadResult(result.notifications)
                }

                is AccountNotificationMutationResult.Failure -> {
                    setState {
                        copy(
                            isNotificationsLoading = false,
                            hubError = result.error,
                        )
                    }
                }
            }
        }
    }

    private fun trackAccountAction(
        action: String,
        params: Map<String, String> = emptyMap(),
    ) {
        analyticsTracker.track(
            AnalyticsEvents.uiAction(
                screenName = SCREEN_NAME,
                action = action,
                params = params,
            )
        )
    }
}

private const val SCREEN_NAME = "account"
