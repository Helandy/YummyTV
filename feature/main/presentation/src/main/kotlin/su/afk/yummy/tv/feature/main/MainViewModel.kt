package su.afk.yummy.tv.feature.main

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleUpdateObserver
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.feature.main.handler.MainSideEffectsHandler
import su.afk.yummy.tv.feature.main.handler.MainUpdateCheckResult
import su.afk.yummy.tv.feature.main.presentation.R
import su.afk.yummy.tv.feature.main.utils.NOTIFICATION_REFRESH_INTERVAL
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject internal constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val analytics: MainAnalytics,
    private val settingsStore: SettingsStore,
    private val nav: INavigationManager,
    private val featureToggleUpdateObserver: FeatureToggleUpdateObserver,
    private val observeAccountSession: ObserveAccountSessionUseCase,
    private val mainSideEffectsHandler: MainSideEffectsHandler,
    private val accountMutationErrorNotifier: AccountMutationErrorNotifier,
    private val stringProvider: StringProvider,
) : BaseViewModel<MainState.State, MainState.Event, MainState.Effect>() {

    override fun createInitialState() = MainState.State()

    override fun onEvent(event: MainState.Event) {
        when (event) {
            is MainState.Event.TvRootSelected -> nav.switchRoot(
                root = event.root,
                reselectPopToRoot = false,
            )
        }
    }

    private var notificationCountsJob: Job? = null

    init {
        analytics.eventScreenOpened()
        observeSettings()
        observeFeatureToggleUpdates()
        observeAccountMutationErrors()
        refreshAccountIfNeeded()
        checkForUpdates()
    }

    private fun observeAccountMutationErrors() {
        accountMutationErrorNotifier.events
            .onEach {
                setEffect(
                    MainState.Effect.ShowToast(
                        stringProvider.get(R.string.main_mutation_error_toast)
                    )
                )
            }
            .launchIn(viewModelScope)
    }

    private fun observeSettings() {
        val yaniApplicationTokenState =
            settingsStore.yaniApplicationTokenState.distinctUntilChanged()
        settingsStore.mainSettingsSnapshot
            .onEach { snapshot ->
                setState {
                    copy(
                        appTheme = snapshot.appTheme,
                        backgroundStyle = snapshot.backgroundStyle,
                        posterQuality = snapshot.posterQuality,
                        posterCardSize = snapshot.posterCardSize,
                        yaniNickname = snapshot.yaniNickname,
                        yaniAvatarUrl = snapshot.yaniAvatarUrl,
                        unreadNotificationsCount = snapshot.yaniUnreadNotificationsCount,
                    )
                }
            }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            combine(
                observeAccountSession(),
                yaniApplicationTokenState,
            ) { session, tokenState ->
                session to tokenState
            }.collect { (session, tokenState) ->
                val signedIn = session.isAuthorized
                analytics.eventAppSession(
                    isAuthorized = signedIn,
                    yaniApplicationTokenState = tokenState,
                )
                setState {
                    copy(
                        isYaniSignedIn = signedIn,
                        isYaniAuthResolved = true,
                        unreadNotificationsCount = if (signedIn) unreadNotificationsCount else 0,
                    )
                }
                observeNotificationCounts(signedIn)
            }
        }
    }

    private fun observeNotificationCounts(signedIn: Boolean) {
        notificationCountsJob?.cancel()
        if (!signedIn) return
        notificationCountsJob = viewModelScope.launch {
            while (isActive) {
                mainSideEffectsHandler.fetchAndPersistNotificationCount()
                delay(NOTIFICATION_REFRESH_INTERVAL)
            }
        }
    }

    private fun observeFeatureToggleUpdates() {
        val initialActivationId = featureToggleUpdateObserver.currentActivationId
        featureToggleUpdateObserver.updates
            .filter { activationId -> activationId > initialActivationId }
            .onEach { checkForUpdates() }
            .launchIn(viewModelScope)
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            when (val result = mainSideEffectsHandler.checkForUpdates()) {
                is MainUpdateCheckResult.Available -> setEffect(
                    MainState.Effect.NavigateToUpdate(
                        version = result.version,
                        apkUrl = result.apkUrl,
                        changelog = result.changelog,
                        required = result.required,
                        updatesCount = result.updatesCount,
                        isPrerelease = result.isPrerelease,
                    )
                )

                MainUpdateCheckResult.NotAvailable -> Unit
            }
        }
    }

    private fun refreshAccountIfNeeded() {
        viewModelScope.launch {
            mainSideEffectsHandler.refreshAccountIfStale()
        }
    }
}
