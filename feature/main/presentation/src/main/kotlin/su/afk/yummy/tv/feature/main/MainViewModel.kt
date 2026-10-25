package su.afk.yummy.tv.feature.main

import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme
import su.afk.yummy.tv.core.model.seasonal.seasonalAnnouncementKey
import su.afk.yummy.tv.core.model.seasonal.seasonalThemeAt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleUpdateObserver
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.network.connectivity.NetworkConnectivityMonitor
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceModePreferences
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.account.repository.AccountMutationErrorRepository
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.domain.library.usecase.ObserveLibraryListStatusesUseCase
import su.afk.yummy.tv.domain.library.usecase.ObserveLibraryUserRatingsUseCase
import su.afk.yummy.tv.feature.main.handler.MainSideEffectsHandler
import su.afk.yummy.tv.feature.main.handler.MainUpdateCheckResult
import su.afk.yummy.tv.feature.main.presentation.R
import su.afk.yummy.tv.feature.playersetup.navigator.PlayerSetupDestination
import su.afk.yummy.tv.feature.update.navigator.UpdateDestination
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
    private val accountMutationErrorRepository: AccountMutationErrorRepository,
    private val stringProvider: StringProvider,
    private val networkConnectivityMonitor: NetworkConnectivityMonitor,
    private val interfaceModePreferences: AppInterfaceModePreferences,
    private val observeLibraryListStatuses: ObserveLibraryListStatusesUseCase,
    private val observeLibraryUserRatings: ObserveLibraryUserRatingsUseCase,
    private val appClock: AppClock,
) : BaseViewModel<MainState.State, MainState.Event, MainState.Effect>() {

    override fun createInitialState() = MainState.State()

    override fun onEvent(event: MainState.Event) {
        when (event) {
            is MainState.Event.RootSelected -> nav.switchRoot(
                root = event.root,
                reselectPopToRoot = event.popToRootOnReselect,
            )

            is MainState.Event.SeasonalPromptAnswered -> viewModelScope.launch {
                if (event.disable) settingsStore.setSeasonalDecorationsEnabled(false)
                settingsStore.setSeasonalAnnouncedKey(seasonalAnnouncementKey(appClock.nowMillis()))
            }
        }
    }

    init {
        analytics.eventScreenOpened()
        observeSettings()
        observeConnectivity()
        observeListStatuses()
        observeFeatureToggleUpdates()
        observeAccountMutationErrors()
        refreshAccountIfNeeded()
        openPlayerSetupIfPending()
        checkForUpdates()
    }

    /** Первичная настройка плеера показывается один раз — сразу после первого выбора интерфейса. */
    private fun openPlayerSetupIfPending() {
        if (interfaceModePreferences.consumePlayerSetupPending()) {
            nav.navigate(PlayerSetupDestination)
        }
    }

    private fun observeConnectivity() {
        networkConnectivityMonitor.isOnline
            .onEach { isOnline -> setState { copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    private fun observeListStatuses() {
        observeLibraryListStatuses.invoke()
            .onEach { statuses -> setState { copy(libraryListStatuses = statuses) } }
            .launchIn(viewModelScope)
        observeLibraryUserRatings.invoke()
            .onEach { ratings -> setState { copy(libraryUserRatings = ratings) } }
            .launchIn(viewModelScope)
    }

    private fun observeAccountMutationErrors() {
        accountMutationErrorRepository.events
            .onEach {
                setEffect(MainState.Effect.ShowToast(stringProvider.get(R.string.main_mutation_error_toast)))
            }
            .launchIn(viewModelScope)
    }

    private fun observeSettings() {
        val yaniApplicationTokenState =
            settingsStore.yaniApplicationTokenState.distinctUntilChanged()
        settingsStore.mainSettingsSnapshot
            .onEach { snapshot ->
                val nowMillis = appClock.nowMillis()
                val seasonalPrompt = seasonalThemeAt(nowMillis).takeIf { theme ->
                    theme != SeasonalTheme.NONE &&
                        snapshot.seasonalDecorationsEnabled &&
                        snapshot.seasonalAnnouncedKey != seasonalAnnouncementKey(nowMillis)
                }
                setState {
                    copy(
                        seasonalPrompt = seasonalPrompt,
                        appTheme = snapshot.appTheme,
                        backgroundStyle = snapshot.backgroundStyle,
                        seasonalDecorationsEnabled = snapshot.seasonalDecorationsEnabled,
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
                if (signedIn && session.userId > 0) {
                    launch { mainSideEffectsHandler.syncLibrary(session.userId) }
                    launch { mainSideEffectsHandler.refreshUnreadNotifications() }
                }
                setState {
                    copy(
                        isYaniSignedIn = signedIn,
                        isYaniAuthResolved = true,
                        unreadNotificationsCount = if (signedIn) unreadNotificationsCount else 0,
                    )
                }
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

    private var updateCheckJob: Job? = null

    private fun checkForUpdates() {
        if (updateCheckJob?.isActive == true) return
        updateCheckJob = viewModelScope.launch {
            when (val result = mainSideEffectsHandler.checkForUpdates()) {
                is MainUpdateCheckResult.Available -> {
                    val destination = UpdateDestination(
                        version = result.version,
                        apkUrl = result.apkUrl,
                        changelog = result.changelog,
                        required = result.required,
                        updatesCount = result.updatesCount,
                        isPrerelease = result.isPrerelease,
                    )
                    // Обязательное обновление заменяет текущий экран: уйти назад с него нельзя.
                    if (result.required) nav.replace(destination) else nav.navigate(destination)
                }

                MainUpdateCheckResult.NotAvailable -> Unit
            }
        }
    }

    private fun refreshAccountIfNeeded() {
        viewModelScope.launch {
            mainSideEffectsHandler.restoreAccountIfMissing()
            mainSideEffectsHandler.refreshAccountIfStale()
        }
    }
}
