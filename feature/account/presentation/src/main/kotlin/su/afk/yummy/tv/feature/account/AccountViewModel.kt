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
import su.afk.yummy.tv.domain.account.model.AccountCaptchaRequiredException
import su.afk.yummy.tv.domain.account.usecase.DeleteNotificationUseCase
import su.afk.yummy.tv.domain.account.usecase.GetNotificationCountsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetProfileNotificationsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserStatsUseCase
import su.afk.yummy.tv.domain.account.usecase.LoginUseCase
import su.afk.yummy.tv.domain.account.usecase.LogoutUseCase
import su.afk.yummy.tv.domain.account.usecase.MarkAllNotificationsReadUseCase
import su.afk.yummy.tv.domain.account.usecase.MarkNotificationReadUseCase
import su.afk.yummy.tv.domain.account.usecase.RefreshAccountUseCase
import su.afk.yummy.tv.domain.account.usecase.ResolveNotificationAnimeIdUseCase
import su.afk.yummy.tv.feature.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.utils.loginCredentialsOrNull
import su.afk.yummy.tv.feature.account.utils.totalUnreadCount
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val settingsStore: SettingsStore,
    private val yaniAuthPreferences: YaniAuthPreferences,
    private val login: LoginUseCase,
    private val logout: LogoutUseCase,
    private val refreshAccount: RefreshAccountUseCase,
    private val getUserStats: GetUserStatsUseCase,
    private val getNotifications: GetProfileNotificationsUseCase,
    private val getNotificationCounts: GetNotificationCountsUseCase,
    private val resolveNotificationAnimeId: ResolveNotificationAnimeIdUseCase,
    private val markNotificationRead: MarkNotificationReadUseCase,
    private val markAllNotificationsRead: MarkAllNotificationsReadUseCase,
    private val deleteNotification: DeleteNotificationUseCase,
    private val detailsNavigator: IDetailsNavigator,
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
                runCatching { logout() }.fold(
                    onSuccess = {
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
                    },
                    onFailure = {
                        setState {
                            copy(
                                isLoading = false,
                                error = AccountUiError.LOGOUT_FAILED
                            )
                        }
                    },
                )
            }
            AccountState.Event.RefreshProfileSelected -> viewModelScope.launch {
                setState { copy(isLoading = true, error = null) }
                runCatching { refreshAccount() }.fold(
                    onSuccess = {
                        setState { copy(isLoading = false) }
                        loadedUserId = 0
                        maybeLoadHub(force = true)
                    },
                    onFailure = {
                        setState {
                            copy(
                                isLoading = false,
                                error = AccountUiError.REFRESH_FAILED
                            )
                        }
                    },
                )
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
                markNotificationRead(event.id)
            }
            AccountState.Event.AllNotificationsReadSelected -> markAllNotificationsReadOptimistically()
            is AccountState.Event.NotificationDeleteSelected -> updateNotification(event.id) {
                deleteNotification(event.id)
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
            runCatching { resolveNotificationAnimeId(slug) }.fold(
                onSuccess = { animeId ->
                    if (animeId != null) {
                        nav.navigate(detailsNavigator.getDetailsDest(animeId))
                    } else {
                        setState { copy(hubError = AccountUiError.OPEN_NOTIFICATION_FAILED) }
                    }
                },
                onFailure = { setState { copy(hubError = AccountUiError.OPEN_NOTIFICATION_FAILED) } },
            )
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
            runCatching { login(credentials.login, credentials.password, captchaResponse) }.fold(
                onSuccess = { account ->
                    loadedUserId = 0
                    missingProfileRefreshAttempted = false
                    isMissingProfileRefreshRunning = false
                    setState {
                        copy(
                            isLoading = false,
                            isSignedIn = account.id > 0,
                            password = "",
                            isCaptchaRequired = false,
                            captchaChallengeId = captchaChallengeId + 1,
                            captchaError = null,
                            userId = account.id,
                            nickname = account.nickname,
                            avatarUrl = account.avatarUrl.orEmpty(),
                        )
                    }
                    maybeLoadHub(force = true)
                },
                onFailure = { error ->
                    if (error is AccountCaptchaRequiredException) {
                        setState {
                            copy(
                                isLoading = false,
                                isCaptchaRequired = true,
                                captchaChallengeId = captchaChallengeId + 1,
                                captchaError = if (captchaResponse == null) {
                                    null
                                } else {
                                    AccountUiError.CAPTCHA_REJECTED
                                },
                                error = null,
                            )
                        }
                        return@fold
                    }
                    setState { copy(isLoading = false, error = AccountUiError.SIGN_IN_FAILED) }
                },
            )
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
            runCatching { refreshAccount() }.fold(
                onSuccess = { account ->
                    isMissingProfileRefreshRunning = false
                    if (account == null) {
                        setState { copy(isLoading = false, isSignedIn = false) }
                    } else {
                        loadedUserId = 0
                        setState {
                            copy(
                                isLoading = false,
                                isSignedIn = account.id > 0,
                                userId = account.id,
                                nickname = account.nickname,
                                avatarUrl = account.avatarUrl.orEmpty(),
                            )
                        }
                        maybeLoadHub(force = true)
                    }
                },
                onFailure = {
                    isMissingProfileRefreshRunning = false
                    setState {
                        copy(
                            isLoading = false,
                            isSignedIn = false,
                            error = AccountUiError.REFRESH_FAILED
                        )
                    }
                },
            )
        }
    }

    private fun maybeLoadHub(force: Boolean = false) {
        val state = currentState
        if (!state.isSignedIn || state.userId <= 0) return
        if (!force && loadedUserId == state.userId) return
        loadedUserId = state.userId
        viewModelScope.launch {
            setState { copy(isStatsLoading = true, isNotificationsLoading = true, hubError = null) }
            runCatching { getUserStats(state.userId) }.fold(
                onSuccess = { stats -> setState { copy(stats = stats, isStatsLoading = false) } },
                onFailure = {
                    setState {
                        copy(
                            isStatsLoading = false,
                            hubError = AccountUiError.LOAD_PROFILE_STATISTICS_FAILED,
                        )
                    }
                },
            )
            loadNotifications()
        }
    }

    private suspend fun loadNotifications() {
        runCatching {
            getNotifications(limit = 20) to getNotificationCounts()
        }.fold(
            onSuccess = { (notifications, counts) ->
                settingsStore.setYaniUnreadNotificationsCount(counts.totalUnreadCount())
                setState {
                    copy(
                        notifications = notifications,
                        notificationCounts = counts,
                        isNotificationsLoading = false,
                    )
                }
            },
            onFailure = {
                setState {
                    copy(
                        isNotificationsLoading = false,
                        hubError = AccountUiError.LOAD_NOTIFICATIONS_FAILED,
                    )
                }
            },
        )
    }

    private fun updateNotification(id: Int, action: suspend () -> Boolean) {
        viewModelScope.launch {
            setState { copy(isNotificationsLoading = true, hubError = null) }
            runCatching { action() }.fold(
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
            runCatching {
                val updated = markAllNotificationsRead()
                if (updated) {
                    settingsStore.setYaniUnreadNotificationsCount(0)
                }
                updated
            }.fold(
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
