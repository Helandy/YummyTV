package su.afk.yummy.tv.feature.main

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.analytics.AnalyticsEvents
import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.featuretoggle.FeatureToggleUpdateObserver
import su.afk.yummy.tv.core.featuretoggle.VersionSupportChecker
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.preferences.settings.YaniApplicationTokenState
import su.afk.yummy.tv.core.update.github.GitHubUpdateChecker
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.account.usecase.GetNotificationCountsUseCase
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.domain.account.usecase.RefreshAccountUseCase
import su.afk.yummy.tv.feature.main.presentation.R
import su.afk.yummy.tv.feature.main.utils.NOTIFICATION_REFRESH_INTERVAL_MS
import su.afk.yummy.tv.feature.main.utils.firstOrZero
import su.afk.yummy.tv.feature.main.utils.isNewer
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class MainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val analyticsTracker: AnalyticsTracker,
    private val settingsStore: SettingsStore,
    private val nav: NavigationManager,
    private val updateChecker: GitHubUpdateChecker,
    private val versionSupportChecker: VersionSupportChecker,
    private val featureToggleUpdateObserver: FeatureToggleUpdateObserver,
    private val observeAccountSession: ObserveAccountSessionUseCase,
    private val getAccountSession: GetAccountSessionUseCase,
    private val refreshAccount: RefreshAccountUseCase,
    private val getNotificationCounts: GetNotificationCountsUseCase,
    private val accountMutationErrorNotifier: AccountMutationErrorNotifier,
    private val stringProvider: StringProvider,
    @param:Named("appVersionName") private val versionName: String,
) : BaseViewModelNew<MainState.State, MainState.Event, MainState.Effect>(savedStateHandle) {

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
        analyticsTracker.track(EVENT_SCREEN_OPENED)
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
                analyticsTracker.track(
                    AnalyticsEvents.appSession(
                        isAuthorized = signedIn,
                        yaniApplicationTokenState = tokenState.analyticsValue(),
                    )
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
            while (true) {
                runCatching { getNotificationCounts().sumOf { it.count } }
                    .onSuccess { count -> settingsStore.setYaniUnreadNotificationsCount(count) }
                delay(NOTIFICATION_REFRESH_INTERVAL_MS)
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
            runCatching {
                val isCurrentVersionSupported = versionSupportChecker.isCurrentVersionSupported()
                val release = updateChecker.getLatestRelease() ?: return@launch
                val remoteVersion = release.tagName.trimStart('v')
                val apkUrl = release.assets.firstOrNull()?.browserDownloadUrl ?: return@launch
                if (!isCurrentVersionSupported || isNewer(versionName, remoteVersion)) {
                    setEffect(
                        MainState.Effect.NavigateToUpdate(
                            version = remoteVersion,
                            apkUrl = apkUrl,
                            changelog = release.body.orEmpty(),
                            required = !isCurrentVersionSupported,
                        )
                    )
                }
            }
        }
    }

    private fun refreshAccountIfNeeded() {
        viewModelScope.launch {
            if (!getAccountSession().isAuthorized) return@launch
            val refreshedAt = settingsStore.yaniTokenRefreshAt.firstOrZero()
            val ageMs = System.currentTimeMillis() - refreshedAt
            if (ageMs > 48 * 60 * 60 * 1000L) {
                runCatching { refreshAccount() }
            }
        }
    }
}

private fun YaniApplicationTokenState.analyticsValue(): String =
    when (this) {
        YaniApplicationTokenState.DEFAULT -> AnalyticsEvents.YANI_APPLICATION_TOKEN_STATE_DEFAULT
        YaniApplicationTokenState.CUSTOM -> AnalyticsEvents.YANI_APPLICATION_TOKEN_STATE_CUSTOM
    }

private const val EVENT_SCREEN_OPENED = "main_screen"
