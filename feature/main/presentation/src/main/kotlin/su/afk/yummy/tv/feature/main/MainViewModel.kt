package su.afk.yummy.tv.feature.main

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.MainMenuFocusTarget
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.update.github.GitHubUpdateChecker
import su.afk.yummy.tv.domain.account.usecase.GetNotificationCountsUseCase
import su.afk.yummy.tv.domain.account.usecase.RefreshAccountUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.main.utils.NOTIFICATION_REFRESH_INTERVAL_MS
import su.afk.yummy.tv.feature.main.utils.firstOrEmpty
import su.afk.yummy.tv.feature.main.utils.firstOrZero
import su.afk.yummy.tv.feature.main.utils.isNewer
import su.afk.yummy.tv.feature.settings.ISettingsNavigator
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class MainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val settingsStore: SettingsStore,
    private val yaniAuthPreferences: YaniAuthPreferences,
    private val nav: NavigationManager,
    private val settingsNavigator: ISettingsNavigator,
    private val accountNavigator: IAccountNavigator,
    private val updateChecker: GitHubUpdateChecker,
    private val refreshAccount: RefreshAccountUseCase,
    private val getNotificationCounts: GetNotificationCountsUseCase,
    @param:Named("appVersionName") private val versionName: String,
) : BaseViewModelNew<MainState.State, MainState.Event, MainState.Effect>(savedStateHandle) {

    override fun createInitialState() = MainState.State()

    override fun onEvent(event: MainState.Event) {
        when (event) {
            is MainState.Event.TvRouteMenuTargetChanged -> setState {
                val hasExplicitMenuFocus = tvMenu.pendingMenuFocusTarget != null ||
                        tvMenu.suppressedFocusSelectionTarget != null
                copy(
                    tvMenu = if (hasExplicitMenuFocus) {
                        tvMenu
                    } else {
                        tvMenu.copy(currentMenuFocusTarget = event.target)
                    }
                )
            }

            is MainState.Event.TvMenuTargetFocused -> onTvMenuTargetFocused(event.target)

            is MainState.Event.TvMenuFocusRequested -> setState {
                copy(
                    tvMenu = tvMenu.copy(
                        currentMenuFocusTarget = event.target,
                        pendingMenuFocusTarget = event.target,
                        suppressedFocusSelectionTarget = event.target,
                        menuFocusRequestId = tvMenu.menuFocusRequestId + 1,
                    )
                )
            }

            is MainState.Event.TvMenuFocusConsumed -> setState {
                copy(
                    tvMenu = tvMenu.copy(
                        pendingMenuFocusTarget = if (tvMenu.pendingMenuFocusTarget == event.target) {
                            null
                        } else {
                            tvMenu.pendingMenuFocusTarget
                        },
                        suppressedFocusSelectionTarget = if (tvMenu.suppressedFocusSelectionTarget == event.target) {
                            null
                        } else {
                            tvMenu.suppressedFocusSelectionTarget
                        },
                    )
                )
            }

            is MainState.Event.TvTabFocused -> {
                onTvMenuTargetFocused(MainMenuFocusTarget.SELECTED_TAB)
                nav.switchTab(event.tab)
            }

            is MainState.Event.TvTabActivated -> {
                nav.switchTab(event.tab)
                requestTvContentFocusFromMenu()
            }

            MainState.Event.TvSettingsFocused -> onTvActionFocused(
                target = MainMenuFocusTarget.SETTINGS_ACTION,
                navigate = { nav.navigate(settingsNavigator.getSettingsDest()) },
            )

            MainState.Event.TvSettingsActivated -> {
                nav.navigate(settingsNavigator.getSettingsDest())
                requestTvContentFocusFromMenu()
            }

            MainState.Event.TvAccountFocused -> onTvActionFocused(
                target = MainMenuFocusTarget.ACCOUNT_ACTION,
                navigate = { nav.navigate(accountNavigator.getAccountDest()) },
            )

            MainState.Event.TvAccountActivated -> {
                nav.navigate(accountNavigator.getAccountDest())
                requestTvContentFocusFromMenu()
            }

            MainState.Event.TvContentFocusRequestedFromMenu -> requestTvContentFocusFromMenu()
        }
    }

    private fun onTvMenuTargetFocused(target: MainMenuFocusTarget) {
        setState {
            copy(
                tvMenu = tvMenu.copy(
                    currentMenuFocusTarget = target,
                    pendingMenuFocusTarget = null,
                    suppressedFocusSelectionTarget = null,
                )
            )
        }
    }

    private fun onTvActionFocused(
        target: MainMenuFocusTarget,
        navigate: () -> Unit,
    ) {
        val isSuppressed = currentState.tvMenu.suppressedFocusSelectionTarget == target
        onTvMenuTargetFocused(target)
        if (!isSuppressed) navigate()
    }

    private fun requestTvContentFocusFromMenu() {
        setState {
            copy(
                tvMenu = tvMenu.copy(
                    pendingMenuFocusTarget = null,
                    suppressedFocusSelectionTarget = null,
                    contentFocusRequestId = tvMenu.contentFocusRequestId + 1,
                )
            )
        }
    }

    private var notificationCountsJob: Job? = null

    init {
        observeSettings()
        refreshAccountIfNeeded()
        checkForUpdates()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsStore.appTheme.collect { setState { copy(appTheme = it) } }
        }
        viewModelScope.launch {
            settingsStore.posterQuality.collect { setState { copy(posterQuality = it) } }
        }
        viewModelScope.launch {
            settingsStore.showScreenshotsOnFocus.collect { setState { copy(showScreenshotsOnFocus = it) } }
        }
        viewModelScope.launch {
            settingsStore.yaniNickname.collect { nickname -> setState { copy(yaniNickname = nickname) } }
        }
        viewModelScope.launch {
            settingsStore.yaniAvatarUrl.collect { avatarUrl -> setState { copy(yaniAvatarUrl = avatarUrl) } }
        }
        viewModelScope.launch {
            settingsStore.yaniUnreadNotificationsCount.collect { count ->
                setState { copy(unreadNotificationsCount = count) }
            }
        }
        viewModelScope.launch {
            yaniAuthPreferences.refreshToken.collect { token ->
                val signedIn = token.isNotBlank()
                setState {
                    copy(
                        isYaniSignedIn = signedIn,
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

    private fun checkForUpdates() {
        viewModelScope.launch {
            runCatching {
                val release = updateChecker.getLatestRelease() ?: return@launch
                val remoteVersion = release.tagName.trimStart('v')
                if (isNewer(versionName, remoteVersion)) {
                    val apkUrl = release.assets.firstOrNull()?.browserDownloadUrl ?: return@launch
                    setEffect(
                        MainState.Effect.NavigateToUpdate(
                            version = remoteVersion,
                            apkUrl = apkUrl,
                            changelog = release.body.orEmpty(),
                        )
                    )
                }
            }
        }
    }

    private fun refreshAccountIfNeeded() {
        viewModelScope.launch {
            val token = yaniAuthPreferences.refreshToken.firstOrEmpty()
            if (token.isBlank()) return@launch
            val refreshedAt = settingsStore.yaniTokenRefreshAt.firstOrZero()
            val ageMs = System.currentTimeMillis() - refreshedAt
            if (ageMs > 48 * 60 * 60 * 1000L) {
                runCatching { refreshAccount() }
            }
        }
    }
}
