package su.afk.yummy.tv.feature.main

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.storage.settings.SettingsStore
import su.afk.yummy.tv.core.update.github.GitHubUpdateChecker
import su.afk.yummy.tv.feature.main.utils.isNewer
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class MainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val settingsStore: SettingsStore,
    private val updateChecker: GitHubUpdateChecker,
    @param:Named("appVersionName") private val versionName: String,
) : BaseViewModelNew<MainState.State, MainState.Event, MainState.Effect>(savedStateHandle) {

    override fun createInitialState() = MainState.State()

    override fun onEvent(event: MainState.Event) {}

    init {
        observeSettings()
        checkForUpdates()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsStore.posterQuality.collect { setState { copy(posterQuality = it) } }
        }
        viewModelScope.launch {
            settingsStore.showScreenshotsOnFocus.collect { setState { copy(showScreenshotsOnFocus = it) } }
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
}
