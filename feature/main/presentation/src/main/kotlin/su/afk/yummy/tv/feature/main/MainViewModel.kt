package su.afk.yummy.tv.feature.main

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.update.github.GitHubUpdateChecker
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier
import su.afk.yummy.tv.domain.account.usecase.GetNotificationCountsUseCase
import su.afk.yummy.tv.domain.account.usecase.RefreshAccountUseCase
import su.afk.yummy.tv.feature.main.presentation.R
import su.afk.yummy.tv.feature.main.utils.NOTIFICATION_REFRESH_INTERVAL_MS
import su.afk.yummy.tv.feature.main.utils.firstOrEmpty
import su.afk.yummy.tv.feature.main.utils.firstOrZero
import su.afk.yummy.tv.feature.main.utils.isNewer
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
    private val updateChecker: GitHubUpdateChecker,
    private val refreshAccount: RefreshAccountUseCase,
    private val getNotificationCounts: GetNotificationCountsUseCase,
    private val accountMutationErrorNotifier: AccountMutationErrorNotifier,
    private val stringProvider: StringProvider,
    @param:Named("appVersionName") private val versionName: String,
) : BaseViewModelNew<MainState.State, MainState.Event, MainState.Effect>(savedStateHandle) {

    override fun createInitialState() = MainState.State()

    override fun onEvent(event: MainState.Event) {
        when (event) {
            is MainState.Event.TvRootFocused -> nav.switchRoot(
                root = event.root,
                reselectPopToRoot = false,
            )
        }
    }

    private var notificationCountsJob: Job? = null

    init {
        observeSettings()
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
        viewModelScope.launch {
            settingsStore.appTheme.collect { setState { copy(appTheme = it) } }
        }
        viewModelScope.launch {
            settingsStore.posterQuality.collect { setState { copy(posterQuality = it) } }
        }
        viewModelScope.launch {
            settingsStore.posterCardSize.collect { setState { copy(posterCardSize = it) } }
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
