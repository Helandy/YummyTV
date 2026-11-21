package su.afk.yummy.tv.feature.account.account

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.domain.account.model.LocalAuthError
import su.afk.yummy.tv.domain.account.model.LocalAuthServerState
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.account.account.handler.AccountHubHandler
import su.afk.yummy.tv.feature.account.account.handler.AccountLocalAuthHandler
import su.afk.yummy.tv.feature.account.account.handler.AccountLoginResult
import su.afk.yummy.tv.feature.account.navigator.AccountNotificationsDestination
import su.afk.yummy.tv.feature.account.account.handler.AccountRefreshResult
import su.afk.yummy.tv.feature.account.account.handler.AccountSessionHandler
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.localauth.LocalAuthAnalytics
import su.afk.yummy.tv.feature.account.utils.loginCredentialsOrNull
import su.afk.yummy.tv.feature.faq.IFaqNavigator
import su.afk.yummy.tv.feature.pages.ISitePagesNavigator
import su.afk.yummy.tv.feature.settings.ISettingsNavigator
import su.afk.yummy.tv.feature.videodownload.IVideoDownloadNavigator
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject internal constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val settingsStore: YaniAccountSettingsStore,
    private val observeAccountSession: ObserveAccountSessionUseCase,
    private val videoDownloadNavigator: IVideoDownloadNavigator,
    private val accountNavigator: IAccountNavigator,
    private val faqNavigator: IFaqNavigator,
    private val sitePagesNavigator: ISitePagesNavigator,
    private val settingsNavigator: ISettingsNavigator,
    private val sessionHandler: AccountSessionHandler,
    private val hubHandler: AccountHubHandler,
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
                            isSessionResolved = true,
                            isSignedIn = false,
                            userId = 0,
                            profileSummary = null,
                            stats = null,
                            unreadNotificationCount = 0,
                            hubError = null,
                            localAuthServerState = LocalAuthServerState.Idle,
                        )
                    } else {
                        copy(
                            isSessionResolved = true,
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
        settingsStore.yaniUnreadNotificationsCount
            .onEach { setState { copy(unreadNotificationCount = it) } }
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
                    setState { copy(error = AccountUiError.CAPTCHA_RESPONSE_EMPTY, errorMessage = null) }
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
                setState { copy(isLoading = true, isLogoutInProgress = true, error = null, errorMessage = null) }
                if (sessionHandler.logout()) {
                    setState {
                        copy(
                            isLoading = false,
                            isLogoutInProgress = false,
                            password = "",
                            selectedTab = AccountState.AccountTab.STATS,
                            profileSummary = null,
                            stats = null,
                            unreadNotificationCount = 0,
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
                            isLogoutInProgress = false,
                            error = AccountUiError.LOGOUT_FAILED,
                            errorMessage = null,
                        )
                    }
                }
            }

            AccountState.Event.RefreshProfileSelected -> viewModelScope.launch {
                analytics.eventRefreshProfileSelected()
                setState { copy(isLoading = true, error = null, errorMessage = null) }
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
                                errorMessage = null,
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

            AccountState.Event.ScreenShown ->
                // Уже загружали в этой сессии — значит это возврат на экран: даём репозиторию
                // решить по TTL, ходить ли в сеть.
                maybeLoadHub(force = sessionHandler.isHubLoadedFor(currentState.userId))

            AccountState.Event.FaqSelected -> nav.navigate(faqNavigator.getFaqDest())

            AccountState.Event.SitePagesSelected -> nav.navigate(sitePagesNavigator.pages())

            AccountState.Event.SettingsSelected -> nav.navigate(settingsNavigator.getSettingsDest())

            AccountState.Event.NotificationsSelected -> nav.navigate(AccountNotificationsDestination)

            AccountState.Event.ProfileEditSelected -> {
                if (currentState.isSignedIn) nav.navigate(accountNavigator.getProfileEditDest())
            }

            AccountState.Event.PasswordResetSelected ->
                nav.navigate(accountNavigator.getPasswordResetDest())

            AccountState.Event.RegistrationSelected ->
                nav.navigate(accountNavigator.getRegistrationDest())


            AccountState.Event.StartLocalAuthServerSelected -> {
                localAuthAnalytics.eventTvPairingStarted()
                startLocalAuthServer()
            }

            AccountState.Event.RefreshLocalAuthPinSelected -> {
                localAuthAnalytics.eventTvPinRefreshed()
                startLocalAuthServer()
            }

            AccountState.Event.LocalAuthPermissionDenied -> {
                localAuthAnalytics.eventTvPairingFailed(LocalAuthError.PERMISSION_DENIED)
                localAuthHandler.cancelServer()
                setState {
                    copy(
                        localAuthServerState = LocalAuthServerState.Error(
                            LocalAuthError.PERMISSION_DENIED,
                        ),
                    )
                }
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

    private fun login(captchaResponse: String? = null) {
        val credentials = currentState.loginCredentialsOrNull()
        if (credentials == null) {
            setState {
                copy(
                    error = AccountUiError.CREDENTIALS_REQUIRED,
                    errorMessage = null,
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
                    isCaptchaRequired = isCaptchaAttempt && isCaptchaRequired,
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
                            errorMessage = null,
                        )
                    }
                    setEffect(AccountState.Effect.HideKeyboard)
                    setEffect(AccountState.Effect.ShowCaptchaHint)
                }

                is AccountLoginResult.Failure -> {
                    analytics.eventLoginFailure()
                    setEffect(AccountState.Effect.DiscardAutofill)
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
            setState { copy(isLoading = true, error = null, errorMessage = null) }
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
                            errorMessage = null,
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
            setState { copy(isStatsLoading = true, hubError = null) }
            showCachedHub(state.userId)
            // Каждая часть хаба показывается по мере прихода, не дожидаясь самой медленной.
            coroutineScope {
                val summary = async { hubHandler.loadProfileSummary(state.userId).also(::applyProfileSummary) }
                val stats = async { hubHandler.loadStats(state.userId).also(::applyStats) }
                // Счётчик непрочитанных обновляется через общую настройку, на экране его слушает init.
                launch { hubHandler.loadNotificationCounts() }
                launch { finishStatistics(summary.await(), stats.await()) }
            }
        }
    }

    /** Показывает сохранённое, пусть и просроченное, чтобы экран не ждал сеть; свежее заменит его. */
    private suspend fun showCachedHub(userId: Int) {
        val cached = hubHandler.loadCached(userId)
        setState {
            copy(
                profileSummary = profileSummary ?: cached.profileSummary,
                stats = stats ?: cached.stats,
            )
        }
    }

    private fun applyProfileSummary(result: Result<UserProfileSummary>) {
        result.onSuccess { summary -> setState { copy(profileSummary = summary) } }
    }

    private fun applyStats(result: Result<UserStats>) {
        result.onSuccess { stats -> setState { copy(stats = stats) } }
    }

    private fun finishStatistics(summary: Result<UserProfileSummary>, stats: Result<UserStats>) {
        setState {
            val hasAnyStatistics = profileSummary != null || this.stats != null
            copy(
                isStatsLoading = false,
                hubError = if (!hasAnyStatistics && (summary.isFailure || stats.isFailure)) {
                    hubError ?: AccountUiError.LOAD_PROFILE_STATISTICS_FAILED
                } else {
                    hubError
                },
            )
        }
    }
}
