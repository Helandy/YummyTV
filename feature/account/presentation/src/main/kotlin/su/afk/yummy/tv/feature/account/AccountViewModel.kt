package su.afk.yummy.tv.feature.account

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.navigation.TopBarFocusTarget
import su.afk.yummy.tv.core.storage.settings.SettingsStore
import su.afk.yummy.tv.domain.account.usecase.DeleteNotificationUseCase
import su.afk.yummy.tv.domain.account.usecase.GetNotificationCountsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetProfileNotificationsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserStatsUseCase
import su.afk.yummy.tv.domain.account.usecase.LoginUseCase
import su.afk.yummy.tv.domain.account.usecase.LogoutUseCase
import su.afk.yummy.tv.domain.account.usecase.MarkAllNotificationsReadUseCase
import su.afk.yummy.tv.domain.account.usecase.MarkNotificationReadUseCase
import su.afk.yummy.tv.domain.account.usecase.RefreshAccountUseCase
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val settingsStore: SettingsStore,
    private val login: LoginUseCase,
    private val logout: LogoutUseCase,
    private val refreshAccount: RefreshAccountUseCase,
    private val getUserStats: GetUserStatsUseCase,
    private val getNotifications: GetProfileNotificationsUseCase,
    private val getNotificationCounts: GetNotificationCountsUseCase,
    private val markNotificationRead: MarkNotificationReadUseCase,
    private val markAllNotificationsRead: MarkAllNotificationsReadUseCase,
    private val deleteNotification: DeleteNotificationUseCase,
) : BaseViewModelNew<AccountState.State, AccountState.Event, AccountState.Effect>(savedStateHandle) {

    private companion object {
        const val TAG = "YaniAccount"
    }

    private var loadedUserId: Int = 0

    override fun createInitialState() = AccountState.State()

    init {
        settingsStore.yaniAccessToken
            .onEach { token ->
                setState {
                    if (token.isBlank()) {
                        copy(
                            accessToken = token,
                            stats = null,
                            notifications = emptyList(),
                            notificationCounts = emptyList(),
                            hubError = null,
                        )
                    } else {
                        copy(accessToken = token)
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
                nav.requestTopBarFocus(TopBarFocusTarget.TRAILING_ACTION)
                nav.back()
            }
            is AccountState.Event.TabSelected -> setState { copy(selectedTab = event.tab) }
            is AccountState.Event.LoginChanged -> setState { copy(login = event.login, error = null) }
            is AccountState.Event.PasswordChanged -> setState { copy(password = event.password, error = null) }
            AccountState.Event.LoginSelected -> {
                Log.d(TAG, "LoginSelected loginBlank=${currentState.login.isBlank()} passwordBlank=${currentState.password.isBlank()}")
                login()
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
                                hubError = null,
                            )
                        }
                        nav.requestTopBarFocus(TopBarFocusTarget.TRAILING_ACTION)
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
            is AccountState.Event.NotificationReadSelected -> updateNotification(event.id) {
                markNotificationRead(event.id)
            }
            AccountState.Event.AllNotificationsReadSelected -> updateNotifications {
                markAllNotificationsRead()
            }
            is AccountState.Event.NotificationDeleteSelected -> updateNotification(event.id) {
                deleteNotification(event.id)
            }
        }
    }

    private fun login() {
        val loginValue = currentState.login.trim()
        val passwordValue = currentState.password
        if (loginValue.isBlank() || passwordValue.isBlank()) {
            Log.d(TAG, "Login blocked by local validation")
            setState { copy(error = "Login and password are required") }
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "Calling /profile/login")
            setState { copy(isLoading = true, error = null) }
            runCatching { login(loginValue, passwordValue) }.fold(
                onSuccess = { account ->
                    Log.d(TAG, "Login succeeded userId=${account.id}")
                    loadedUserId = 0
                    setState {
                        copy(
                            isLoading = false,
                            password = "",
                            userId = account.id,
                            nickname = account.nickname,
                            avatarUrl = account.avatarUrl.orEmpty(),
                        )
                    }
                    maybeLoadHub(force = true)
                },
                onFailure = { error ->
                    Log.d(TAG, "Login failed: ${error::class.simpleName}: ${error.message}")
                    val message = if (error::class.simpleName == "YaniCaptchaRequiredException") {
                        "Captcha required. Login on the website first, then try again later."
                    } else {
                        error.message ?: "Could not sign in"
                    }
                    setState { copy(isLoading = false, error = message) }
                },
            )
        }
    }

    private fun maybeLoadHub(force: Boolean = false) {
        val state = currentState
        if (state.accessToken.isBlank() || state.userId <= 0) return
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

    private fun updateNotifications(action: suspend () -> Boolean) {
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
                            hubError = error.message ?: "Could not update notifications",
                        )
                    }
                },
            )
        }
    }
}
