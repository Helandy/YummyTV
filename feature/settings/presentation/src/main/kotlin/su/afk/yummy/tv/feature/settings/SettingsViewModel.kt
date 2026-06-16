package su.afk.yummy.tv.feature.settings

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.tv.api.ITvIntegration
import su.afk.yummy.tv.feature.settings.navigator.SettingsDetailsButtonOrderDestination
import su.afk.yummy.tv.feature.settings.utils.moved
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject internal constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val settingsStore: SettingsStore,
    private val tvIntegration: ITvIntegration,
    private val nav: NavigationManager,
    private val analytics: SettingsAnalytics,
) : BaseViewModelNew<SettingsState.State, SettingsState.Event, SettingsState.Effect>(savedStateHandle) {

    override fun createInitialState() = SettingsState.State()

    init {
        analytics.eventScreenOpened()
        settingsStore.settingsSnapshot
            .onEach { snapshot ->
                setState {
                    copy(
                        appTheme = snapshot.appTheme,
                        posterQuality = snapshot.posterQuality,
                        posterCardSize = snapshot.posterCardSize,
                        preferredPlayer = snapshot.preferredPlayer,
                        watchNextEnabled = snapshot.watchNextEnabled,
                        previewCacheSize = snapshot.previewCacheSize,
                        autoSkipOpeningsEndings = snapshot.autoSkipOpeningsEndings,
                        yaniApplicationToken = snapshot.yaniApplicationToken,
                        contentLanguage = snapshot.contentLanguage,
                        detailsButtonOrder = snapshot.detailsButtonOrder,
                    )
                }
            }
            .launchIn(viewModelScope)
        tvIntegration.previewChannelBrowsable
            .onEach { setState { copy(isPreviewChannelBrowsable = it) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: SettingsState.Event) {
        when (event) {
            SettingsState.Event.BackSelected -> nav.back()
            is SettingsState.Event.AppThemeSelected -> viewModelScope.launch {
                analytics.eventAppThemeSelected(event.theme)
                settingsStore.setAppTheme(event.theme)
            }
            is SettingsState.Event.PosterQualitySelected -> viewModelScope.launch {
                analytics.eventPosterQualitySelected(event.quality)
                settingsStore.setPosterQuality(event.quality)
            }
            is SettingsState.Event.PosterCardSizeSelected -> viewModelScope.launch {
                analytics.eventPosterCardSizeSelected(event.size)
                settingsStore.setPosterCardSize(event.size)
            }
            is SettingsState.Event.PreferredPlayerSelected -> viewModelScope.launch {
                analytics.eventPreferredPlayerSelected(event.player)
                settingsStore.setPreferredPlayer(event.player)
            }
            SettingsState.Event.RequestPreviewChannelBrowsable -> {
                analytics.eventRequestPreviewChannelBrowsable()
                tvIntegration.requestPreviewChannelBrowsable()
            }
            SettingsState.Event.WatchNextToggled -> viewModelScope.launch {
                val enabled = !currentState.watchNextEnabled
                analytics.eventWatchNextToggled(enabled)
                settingsStore.setWatchNextEnabled(enabled)
            }
            is SettingsState.Event.PreviewCacheSizeSelected -> viewModelScope.launch {
                analytics.eventPreviewCacheSizeSelected(event.size)
                settingsStore.setPreviewCacheSize(event.size)
            }
            SettingsState.Event.AutoSkipOpeningsEndingsToggled -> viewModelScope.launch {
                val enabled = !currentState.autoSkipOpeningsEndings
                analytics.eventAutoSkipOpeningsEndingsToggled(enabled)
                settingsStore.setAutoSkipOpeningsEndings(enabled)
            }
            is SettingsState.Event.YaniApplicationTokenChanged -> {
                setState { copy(yaniApplicationToken = event.token) }
                viewModelScope.launch {
                    settingsStore.setYaniApplicationToken(event.token)
                }
            }
            is SettingsState.Event.ContentLanguageSelected -> viewModelScope.launch {
                analytics.eventContentLanguageSelected(event.language)
                settingsStore.setYaniContentLanguage(event.language)
            }
            is SettingsState.Event.DetailsButtonMoved -> viewModelScope.launch {
                analytics.eventDetailsButtonMoved(event.action, event.direction)
                settingsStore.setDetailsButtonOrder(
                    currentState.detailsButtonOrder.moved(event.action, event.direction),
                )
            }
            SettingsState.Event.DetailsButtonOrderScreenOpened -> {
                analytics.eventDetailsButtonOrderScreenOpened()
            }
            SettingsState.Event.DetailsButtonOrderSelected -> {
                nav.navigate(SettingsDetailsButtonOrderDestination)
            }
            SettingsState.Event.DetailsButtonOrderReset -> viewModelScope.launch {
                analytics.eventDetailsButtonOrderReset()
                settingsStore.setDetailsButtonOrder(SettingsStore.defaultDetailsButtonOrder)
            }
        }
    }
}
