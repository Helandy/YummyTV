package su.afk.yummy.tv.feature.account.account

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.EpisodePushSettingsStore
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.domain.account.model.LocalAuthServerState
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.account.account.handler.AccountHubHandler
import su.afk.yummy.tv.feature.account.account.handler.AccountLocalAuthHandler
import su.afk.yummy.tv.feature.account.account.handler.AccountLoginResult
import su.afk.yummy.tv.feature.account.account.handler.AccountNotificationHandler
import su.afk.yummy.tv.feature.account.account.handler.AccountNotificationMutationHandler
import su.afk.yummy.tv.feature.account.account.handler.AccountNotificationMutationOutcome
import su.afk.yummy.tv.feature.account.account.handler.AccountNotificationsLoadResult
import su.afk.yummy.tv.feature.account.account.handler.AccountOpenNotificationResult
import su.afk.yummy.tv.feature.account.account.handler.AccountRefreshResult
import su.afk.yummy.tv.feature.account.account.handler.AccountSessionHandler
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.localauth.LocalAuthAnalytics
import su.afk.yummy.tv.feature.account.utils.decrementCount
import su.afk.yummy.tv.feature.account.utils.loginCredentialsOrNull
import su.afk.yummy.tv.feature.account.utils.totalUnreadCount
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.messages.IMessagesNavigator
import su.afk.yummy.tv.feature.videodownload.IVideoDownloadNavigator
import su.afk.yummy.tv.feature.watchlater.IWatchLaterNavigator
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject internal constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val settingsStore: YaniAccountSettingsStore,
    private val episodePushSettingsStore: EpisodePushSettingsStore,
    private val observeAccountSession: ObserveAccountSessionUseCase,
    private val detailsNavigator: IDetailsNavigator,
    private val videoDownloadNavigator: IVideoDownloadNavigator,
    private val watchLaterNavigator: IWatchLaterNavigator,
    private val accountNavigator: IAccountNavigator,
    private val messagesNavigator: IMessagesNavigator,
    private val sessionHandler: AccountSessionHandler,
    private val hubHandler: AccountHubHandler,
    private val notificationHandler: AccountNotificationHandler,
    private val notificationMutationHandler: AccountNotificationMutationHandler,
    private val localAuthHandler: AccountLocalAuthHandler,
    private val analytics: AccountAnalytics,
    private val localAuthAnalytics: LocalAuthAnalytics,
) : BaseViewModel<AccountState.State, AccountState.Event, AccountState.Effect>() {

    override fun createInitialState() = AccountState.State()

    init {
        analytics.eventScreenOpened()
        observeAccountSession()
            .onEach { session ->
                sessionHandler.onSessionSnapshot(session.isAuthorized)
                val isAuthorized = sessionHandler.isAuthorized()
                setState {
                    if (!isAuthorized) {
                        copy(
                            isSignedIn = false,
                            userId = 0,
                            profileSummary = null,
                            stats = null,
                            notifications = persistentListOf(),
                            notificationCounts = persistentListOf(),
                            isNotificationOpening = false,
                            hubError = null,
                            localAuthServerState = LocalAuthServerState.Idle,
                        )
                    } else {
                        copy(
                            isSignedIn = session.userId > 0,
                            userId = session.userId,
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
        episodePushSettingsStore.pushEnabled
            .onEach { setState { copy(episodePushEnabled = it) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: AccountState.Event) {
        when (event) {
            AccountState.Event.BackSelected -> {
                nav.back()
            }

            is AccountState.Event.TabSelected -> {
                if (event.tab != currentState.selectedTab) {
                    analytics.eventTabSelected(event.tab)
                }
                setState { copy(selectedTab = event.tab) }
            }

            is AccountState.Event.LoginChanged -> setState {
                copy(
                    login = event.login,
                    error = null,
                    errorMessage = null,
                    isCaptchaRequired = false,
                    captchaChallengeId = currentState.captchaChallengeId + 1,
                    captchaError = null,
                )
            }

            is AccountState.Event.PasswordChanged -> setState {
                copy(
                    password = event.password,
                    error = null,
                    errorMessage = null,
                    isCaptchaRequired = false,
                    captchaChallengeId = currentState.captchaChallengeId + 1,
                    captchaError = null,
                )
            }

            AccountState.Event.LoginSelected -> {
                analytics.eventLoginSelected()
                login()
            }

            is AccountState.Event.CaptchaSolved -> {
                setState { copy(error = null, errorMessage = null, captchaError = null) }
                if (event.token.isBlank()) {
                    setState { copy(error = AccountUiError.CAPTCHA_RESPONSE_EMPTY) }
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
                analytics.eventLogoutSelected()
                setState { copy(isLoading = true, error = null) }
                if (sessionHandler.logout()) {
                    setState {
                        copy(
                            isLoading = false,
                            password = "",
                            selectedTab = AccountState.AccountTab.STATS,
                            profileSummary = null,
                            stats = null,
                            notifications = persistentListOf(),
                            notificationCounts = persistentListOf(),
                            isNotificationOpening = false,
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
                            error = AccountUiError.LOGOUT_FAILED,
                        )
                    }
                }
            }

            AccountState.Event.RefreshProfileSelected -> viewModelScope.launch {
                analytics.eventRefreshProfileSelected()
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
                                error = AccountUiError.REFRESH_FAILED,
                            )
                        }
                    }
                }
            }

            AccountState.Event.RefreshHubSelected -> {
                analytics.eventRefreshAccountDataSelected()
                maybeLoadHub(force = true)
            }

            AccountState.Event.DownloadedEpisodesSelected -> {
                nav.navigate(videoDownloadNavigator.getVideoDownloadDest())
            }

            AccountState.Event.WatchLaterSelected -> {
                nav.navigate(watchLaterNavigator.getWatchLaterDest())
            }

            AccountState.Event.MessagesSelected -> {
                if (currentState.isSignedIn) nav.navigate(messagesNavigator.dialogs())
            }

            AccountState.Event.ScreenShown ->
                // Уже загружали в этой сессии — значит это возврат на экран: даём репозиторию
                // решить по TTL, ходить ли в сеть.
                maybeLoadHub(force = sessionHandler.isHubLoadedFor(currentState.userId))

            AccountState.Event.UserSearchSelected ->
                nav.navigate(accountNavigator.getUserSearchDest())

            AccountState.Event.MySubscriptionsSelected -> {
                if (currentState.isSignedIn) nav.navigate(accountNavigator.getMySubscriptionsDest())
            }

            AccountState.Event.EpisodePushToggled -> viewModelScope.launch {
                episodePushSettingsStore.setPushEnabled(!currentState.episodePushEnabled)
            }

            AccountState.Event.ProfileEditSelected -> {
                if (currentState.isSignedIn) nav.navigate(accountNavigator.getProfileEditDest())
            }

            AccountState.Event.PasswordResetSelected ->
                nav.navigate(accountNavigator.getPasswordResetDest())

            AccountState.Event.RegistrationSelected ->
                nav.navigate(accountNavigator.getRegistrationDest())

            is AccountState.Event.NotificationSelected -> openNotification(event.id)
            is AccountState.Event.NotificationReadSelected -> {
                analytics.eventNotificationReadSelected(event.id)
                markNotificationReadOptimistically(event.id)
            }

            AccountState.Event.AllNotificationsReadSelected -> {
                analytics.eventAllNotificationsReadSelected()
                markAllNotificationsReadOptimistically()
            }

            AccountState.Event.AllNotificationsDeleteSelected -> {
                deleteAllNotificationsOptimistically()
            }

            is AccountState.Event.NotificationDeleteSelected -> {
                analytics.eventNotificationDeleteSelected(event.id)
                deleteNotificationOptimistically(event.id)
            }

            AccountState.Event.StartLocalAuthServerSelected -> {
                localAuthAnalytics.eventTvPairingStarted()
                startLocalAuthServer()
            }

            AccountState.Event.RefreshLocalAuthPinSelected -> {
                localAuthAnalytics.eventTvPinRefreshed()
                startLocalAuthServer()
            }

            AccountState.Event.StopLocalAuthServerSelected -> {
                localAuthAnalytics.eventTvPairingCancelled()
                localAuthHandler.stopServer(viewModelScope)
                setState { copy(localAuthServerState = LocalAuthServerState.Idle) }
            }
        }
    }

    private fun startLocalAuthServer() {
        localAuthHandler.startServer(viewModelScope) { serverState ->
            setState { copy(localAuthServerState = serverState) }
            when (serverState) {
                is LocalAuthServerState.Pairing -> trackPairingState(serverState)

                is LocalAuthServerState.Success -> {
                    localAuthAnalytics.eventTvTransferSuccess()
                    // Сессия уже сохранена — сервер и NSD-регистрация больше не нужны.
                    localAuthHandler.cancelServer()
                }

                is LocalAuthServerState.Error -> {
                    localAuthAnalytics.eventTvPairingFailed(serverState.reason)
                    // PIN больше не примут: гасим сервер, панель предложит выпустить новый код.
                    localAuthHandler.cancelServer()
                }

                else -> Unit
            }
        }
    }

    /** Первый [LocalAuthServerState.Pairing] — код на экране, последующие — неудачные попытки. */
    private fun trackPairingState(state: LocalAuthServerState.Pairing) {
        if (state.lastError == null) {
            localAuthAnalytics.eventTvPinShown()
        } else {
            localAuthAnalytics.eventTvWrongPin(state.attemptsLeft)
        }
    }

    private fun openNotification(id: Int) {
        if (currentState.isNotificationOpening) return
        val notification = currentState.notifications.firstOrNull { it.id == id } ?: return
        if (!notification.isNewEpisode) return
        val slug = notification.animeSlug ?: return
        viewModelScope.launch {
            setState { copy(isNotificationOpening = true, hubError = null) }
            when (val result = notificationHandler.resolveAnimeId(slug)) {
                is AccountOpenNotificationResult.Navigate -> {
                    analytics.eventNotificationSelected(notification, result.animeId)
                    setState {
                        copy(
                            isNotificationOpening = false,
                        )
                    }
                    if (!notification.viewed) markNotificationReadOptimistically(notification.id)
                    nav.navigate(detailsNavigator.getDetailsDest(result.animeId))
                }

                AccountOpenNotificationResult.Failure -> {
                    setState {
                        copy(
                            isNotificationOpening = false,
                            hubError = AccountUiError.OPEN_NOTIFICATION_FAILED,
                        )
                    }
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
            val isCaptchaAttempt = captchaResponse != null
            setState {
                copy(
                    isLoading = true,
                    error = null,
                    errorMessage = null,
                    captchaError = null,
                    // Капчу не размонтируем, пока проверяем её же токен.
                    isCaptchaRequired = if (isCaptchaAttempt) isCaptchaRequired else false,
                    captchaChallengeId = if (isCaptchaAttempt) {
                        captchaChallengeId
                    } else {
                        currentState.captchaChallengeId + 1
                    },
                )
            }
            when (val result = sessionHandler.login(credentials, captchaResponse)) {
                is AccountLoginResult.Success -> {
                    analytics.eventLoginSuccess()
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
                    analytics.eventLoginCaptchaRequired(result.rejected)
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
                    setEffect(AccountState.Effect.HideKeyboard)
                    setEffect(AccountState.Effect.ShowCaptchaHint)
                }

                is AccountLoginResult.Failure -> {
                    analytics.eventLoginFailure()
                    setState {
                        copy(
                            isLoading = false,
                            error = AccountUiError.SIGN_IN_FAILED,
                            errorMessage = result.message,
                        )
                    }
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
                            error = AccountUiError.REFRESH_FAILED,
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
                if (result.profileSummary != null) {
                    next = next.copy(profileSummary = result.profileSummary)
                }
                if (result.stats != null) {
                    next = next.copy(stats = result.stats)
                }
                val hasAnyStatistics = next.profileSummary != null || next.stats != null
                if (!hasAnyStatistics) {
                    val error = result.profileSummaryError ?: result.statsError
                    if (error != null) {
                        next = next.copy(hubError = error)
                    }
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
                        notifications = result.notifications.toImmutableList(),
                        notificationCounts = result.counts.toImmutableList(),
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

    // Notification mutations apply to local state immediately so the UI never waits on a
    // network round trip; the server call runs in the background and only a failure re-syncs
    // from `hubHandler`, since a stale response is otherwise indistinguishable from success.

    private fun markNotificationReadOptimistically(id: Int) {
        val notification = currentState.notifications.firstOrNull { it.id == id } ?: return
        if (notification.viewed) return
        val previousNotifications = currentState.notifications
        val previousCounts = currentState.notificationCounts
        val updatedCounts = previousCounts.decrementCount(notification.type)
        setState {
            copy(
                notifications = notifications.map {
                    if (it.id == id) it.copy(viewed = true) else it
                }.toImmutableList(),
                notificationCounts = updatedCounts,
                hubError = null,
            )
        }
        viewModelScope.launch {
            settingsStore.setYaniUnreadNotificationsCount(updatedCounts.totalUnreadCount())
            val outcome = notificationMutationHandler.markNotificationRead(id)
            if (outcome is AccountNotificationMutationOutcome.Failure) {
                revertNotifications(previousNotifications, previousCounts, outcome.error)
            }
        }
    }

    private fun deleteNotificationOptimistically(id: Int) {
        val notification = currentState.notifications.firstOrNull { it.id == id } ?: return
        val previousNotifications = currentState.notifications
        val previousCounts = currentState.notificationCounts
        val updatedCounts = if (notification.viewed) {
            previousCounts
        } else {
            previousCounts.decrementCount(notification.type)
        }
        setState {
            copy(
                notifications = notifications.filterNot { it.id == id }.toImmutableList(),
                notificationCounts = updatedCounts,
                hubError = null,
            )
        }
        viewModelScope.launch {
            if (updatedCounts !== previousCounts) {
                settingsStore.setYaniUnreadNotificationsCount(updatedCounts.totalUnreadCount())
            }
            val outcome = notificationMutationHandler.deleteNotification(id)
            if (outcome is AccountNotificationMutationOutcome.Failure) {
                revertNotifications(previousNotifications, previousCounts, outcome.error)
            }
        }
    }

    private fun markAllNotificationsReadOptimistically() {
        val previousNotifications = currentState.notifications
        val previousCounts = currentState.notificationCounts
        if (previousNotifications.all { it.viewed }) return
        setState {
            copy(
                notifications = notifications.map { it.copy(viewed = true) }.toImmutableList(),
                notificationCounts = notificationCounts.map { it.copy(count = 0) }
                    .toImmutableList(),
                hubError = null,
            )
        }
        viewModelScope.launch {
            settingsStore.setYaniUnreadNotificationsCount(0)
            val outcome = notificationMutationHandler.markAllNotificationsRead()
            if (outcome is AccountNotificationMutationOutcome.Failure) {
                revertNotifications(previousNotifications, previousCounts, outcome.error)
            }
        }
    }

    private fun deleteAllNotificationsOptimistically() {
        val previousNotifications = currentState.notifications
        val previousCounts = currentState.notificationCounts
        if (previousNotifications.isEmpty()) return
        setState {
            copy(
                notifications = persistentListOf(),
                notificationCounts = persistentListOf(),
                hubError = null,
            )
        }
        viewModelScope.launch {
            settingsStore.setYaniUnreadNotificationsCount(0)
            val outcome = notificationMutationHandler.deleteAllNotifications()
            if (outcome is AccountNotificationMutationOutcome.Failure) {
                revertNotifications(previousNotifications, previousCounts, outcome.error)
            }
        }
    }

    private fun revertNotifications(
        notifications: ImmutableList<ProfileNotification>,
        counts: ImmutableList<NotificationCount>,
        error: AccountUiError,
    ) {
        setState {
            copy(
                notifications = notifications,
                notificationCounts = counts,
                hubError = error,
            )
        }
        viewModelScope.launch {
            settingsStore.setYaniUnreadNotificationsCount(counts.totalUnreadCount())
        }
    }
}
