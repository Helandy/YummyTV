package su.afk.yummy.tv.feature.settings

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.storage.settings.SettingsStore
import su.afk.yummy.tv.core.tv.ITvIntegration
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val settingsStore: SettingsStore,
    private val tvIntegration: ITvIntegration,
) : BaseViewModelNew<SettingsState.State, SettingsState.Event, SettingsState.Effect>(savedStateHandle) {

    override fun createInitialState() = SettingsState.State()

    init {
        settingsStore.posterQuality
            .onEach { setState { copy(posterQuality = it) } }
            .launchIn(viewModelScope)
        settingsStore.showScreenshotsOnFocus
            .onEach { setState { copy(showScreenshotsOnFocus = it) } }
            .launchIn(viewModelScope)
        settingsStore.preferredPlayer
            .onEach { setState { copy(preferredPlayer = it) } }
            .launchIn(viewModelScope)
        tvIntegration.previewChannelBrowsable
            .onEach { setState { copy(isPreviewChannelBrowsable = it) } }
            .launchIn(viewModelScope)
        settingsStore.watchNextEnabled
            .onEach { setState { copy(watchNextEnabled = it) } }
            .launchIn(viewModelScope)
        settingsStore.previewCacheSize
            .onEach { setState { copy(previewCacheSize = it) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: SettingsState.Event) {
        when (event) {
            is SettingsState.Event.PosterQualitySelected -> viewModelScope.launch {
                settingsStore.setPosterQuality(event.quality)
            }
            SettingsState.Event.ShowScreenshotsOnFocusToggled -> viewModelScope.launch {
                settingsStore.setShowScreenshotsOnFocus(!currentState.showScreenshotsOnFocus)
            }
            is SettingsState.Event.PreferredPlayerSelected -> viewModelScope.launch {
                settingsStore.setPreferredPlayer(event.player)
            }
            SettingsState.Event.RequestPreviewChannelBrowsable -> {
                tvIntegration.requestPreviewChannelBrowsable()
            }
            SettingsState.Event.WatchNextToggled -> viewModelScope.launch {
                settingsStore.setWatchNextEnabled(!currentState.watchNextEnabled)
            }
            is SettingsState.Event.PreviewCacheSizeSelected -> viewModelScope.launch {
                settingsStore.setPreviewCacheSize(event.size)
            }
        }
    }
}
