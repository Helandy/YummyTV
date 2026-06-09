package su.afk.yummy.tv.feature.account

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
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

    override fun createInitialState() = AccountState.State()

    init {
        yaniAuthPreferences.refreshToken
            .onEach { token ->
                setState {
                    if (token.isBlank()) {
                        copy(
                            isSignedIn = false,
                            stats = null,
                            notifications = emptyList(),
                            notificationCounts = emptyList(),
                            hubError = null,
                        )
                    } else {
                        copy(isSignedIn = true)
                    }
                }
                maybeLoadHub()
            }
            .launchIn(viewModelScope)
        settingsStore.yaniUserId
            .onEach {
                setState { copy(userId = it) }
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
                    setState { copy(captchaError = "Captcha response is empty") }
                } else {
                    login(captchaResponse = event.token)
                }
            }
            AccountState.Event.CaptchaExpired -> setState {
                copy(
                    isLoading = false,
                    captchaChallengeId = currentState.captchaChallengeId + 1,
                    captchaError = "Captcha expired. Try again.",
                )
            }
            is AccountState.Event.CaptchaFailed -> setState {
                copy(
                    isLoading = false,
                    captchaChallengeId = currentState.captchaChallengeId + 1,
                    captchaError = event.message ?: "Could not load captcha. Try again.",
                )
            }
            AccountState.Event.LogoutSelected -> viewModelScope.launch {
                setState { copy(isLoading = true, error = null) }
                runCatching { logout() }.fold(
                    onSuccess = {
                        loadedUserId = 0
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
                    onFailure = { setState { copy(isLoading = false, error = it.message) } },
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
                    onFailure = { setState { copy(isLoading = false, error = it.message) } },
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
                        setState { copy(hubError = "Could not open notification") }
                    }
                },
                onFailure = { error ->
                    setState { copy(hubError = error.message ?: "Could not open notification") }
                },
            )
        }
    }

    private fun login(captchaResponse: String? = null) {
        val loginValue = currentState.login.trim()
        val passwordValue = currentState.password
        if (loginValue.isBlank() || passwordValue.isBlank()) {
            setState {
                copy(
                    error = "Login and password are required",
                    isCaptchaRequired = false,
                    captchaChallengeId = currentState.captchaChallengeId + 1,
                    captchaError = null,
                )
            }
            return
        }
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null, captchaError = null) }
            runCatching { login(loginValue, passwordValue, captchaResponse) }.fold(
                onSuccess = { account ->
                    loadedUserId = 0
                    setState {
                        copy(
                            isLoading = false,
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
                                    "Captcha was not accepted. Try again."
                                },
                                error = null,
                            )
                        }
                        return@fold
                    }
                    setState { copy(isLoading = false, error = error.message ?: "Could not sign in") }
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
                onFailure = { error ->
                    setState {
                        copy(
                            isStatsLoading = false,
                            hubError = error.message ?: "Could not load profile statistics",
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
                settingsStore.setYaniUnreadNotificationsCount(counts.sumOf { it.count })
                setState {
                    copy(
                        notifications = notifications,
                        notificationCounts = counts,
                        isNotificationsLoading = false,
                    )
                }
            },
            onFailure = { error ->
                setState {
                    copy(
                        isNotificationsLoading = false,
                        hubError = error.message ?: "Could not load notifications",
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
                onFailure = { error ->
                    setState {
                        copy(
                            isNotificationsLoading = false,
                            hubError = error.message ?: "Could not update notification",
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
                onFailure = { error ->
                    setState {
                        copy(
                            isNotificationsLoading = false,
                            hubError = error.message ?: "Could not update notifications",
                        )
                    }
                },
            )
        }
    }

}
