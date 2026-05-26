package su.afk.yummy.tv.feature.settings

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.storage.settings.DetailsButtonAction
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
        settingsStore.autoSkipOpeningsEndings
            .onEach { setState { copy(autoSkipOpeningsEndings = it) } }
            .launchIn(viewModelScope)
        settingsStore.yaniApplicationToken
            .onEach { setState { copy(yaniApplicationToken = it) } }
            .launchIn(viewModelScope)
        settingsStore.detailsButtonOrder
            .onEach { setState { copy(detailsButtonOrder = it) } }
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
            SettingsState.Event.AutoSkipOpeningsEndingsToggled -> viewModelScope.launch {
                settingsStore.setAutoSkipOpeningsEndings(!currentState.autoSkipOpeningsEndings)
            }
            is SettingsState.Event.YaniApplicationTokenChanged -> {
                setState { copy(yaniApplicationToken = event.token) }
                viewModelScope.launch {
                    settingsStore.setYaniApplicationToken(event.token)
                }
            }
            is SettingsState.Event.DetailsButtonMoved -> viewModelScope.launch {
                settingsStore.setDetailsButtonOrder(
                    currentState.detailsButtonOrder.moved(event.action, event.direction),
                )
            }
            SettingsState.Event.DetailsButtonOrderReset -> viewModelScope.launch {
                settingsStore.setDetailsButtonOrder(SettingsStore.defaultDetailsButtonOrder)
            }
        }
    }

    private fun List<DetailsButtonAction>.moved(
        action: DetailsButtonAction,
        direction: DetailsButtonMoveDirection,
    ): List<DetailsButtonAction> {
        val groups = toDetailsButtonGroups()
        val index = groups.indexOfFirst { action in it }
        if (index == -1) return this
        val targetIndex = when (direction) {
            DetailsButtonMoveDirection.UP -> index - 1
            DetailsButtonMoveDirection.DOWN -> index + 1
        }
        if (targetIndex !in groups.indices) return this
        return groups.toMutableList().apply {
            this[index] = this[targetIndex]
            this[targetIndex] = groups[index]
        }.flatten()
    }

    private fun List<DetailsButtonAction>.toDetailsButtonGroups(): List<List<DetailsButtonAction>> = buildList {
        var index = 0
        while (index <= this@toDetailsButtonGroups.lastIndex) {
            val action = this@toDetailsButtonGroups[index]
            val nextAction = this@toDetailsButtonGroups.getOrNull(index + 1)
            if (action == DetailsButtonAction.LIBRARY && nextAction == DetailsButtonAction.FAVORITE) {
                add(listOf(action, nextAction))
                index += 2
            } else if (action != DetailsButtonAction.FAVORITE) {
                add(listOf(action))
                index += 1
            } else {
                index += 1
            }
        }
    }
}
