package su.afk.yummy.tv.feature.settings

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.analytics.AnalyticsEvents
import su.afk.yummy.tv.core.analytics.AnalyticsTracker
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
class SettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val settingsStore: SettingsStore,
    private val tvIntegration: ITvIntegration,
    private val nav: NavigationManager,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModelNew<SettingsState.State, SettingsState.Event, SettingsState.Effect>(savedStateHandle) {

    override fun createInitialState() = SettingsState.State()

    init {
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
                trackSettingChange("app_theme", event.theme.name.lowercase())
                settingsStore.setAppTheme(event.theme)
            }
            is SettingsState.Event.PosterQualitySelected -> viewModelScope.launch {
                trackSettingChange("poster_quality", event.quality.name.lowercase())
                settingsStore.setPosterQuality(event.quality)
            }
            is SettingsState.Event.PosterCardSizeSelected -> viewModelScope.launch {
                trackSettingChange("poster_card_size", event.size.name.lowercase())
                settingsStore.setPosterCardSize(event.size)
            }
            is SettingsState.Event.PreferredPlayerSelected -> viewModelScope.launch {
                trackSettingChange("preferred_player", event.player.name.lowercase())
                settingsStore.setPreferredPlayer(event.player)
            }
            SettingsState.Event.RequestPreviewChannelBrowsable -> {
                analyticsTracker.track(
                    AnalyticsEvents.uiAction(
                        screenName = SCREEN_NAME,
                        action = "request_preview_channel_browsable",
                    )
                )
                tvIntegration.requestPreviewChannelBrowsable()
            }
            SettingsState.Event.WatchNextToggled -> viewModelScope.launch {
                val enabled = !currentState.watchNextEnabled
                trackSettingChange("watch_next_enabled", enabled.toString())
                settingsStore.setWatchNextEnabled(enabled)
            }
            is SettingsState.Event.PreviewCacheSizeSelected -> viewModelScope.launch {
                trackSettingChange("preview_cache_size", event.size.name.lowercase())
                settingsStore.setPreviewCacheSize(event.size)
            }
            SettingsState.Event.AutoSkipOpeningsEndingsToggled -> viewModelScope.launch {
                val enabled = !currentState.autoSkipOpeningsEndings
                trackSettingChange("auto_skip_openings_endings", enabled.toString())
                settingsStore.setAutoSkipOpeningsEndings(enabled)
            }
            is SettingsState.Event.YaniApplicationTokenChanged -> {
                setState { copy(yaniApplicationToken = event.token) }
                viewModelScope.launch {
                    settingsStore.setYaniApplicationToken(event.token)
                }
            }
            is SettingsState.Event.ContentLanguageSelected -> viewModelScope.launch {
                trackSettingChange("content_language", event.language.name.lowercase())
                settingsStore.setYaniContentLanguage(event.language)
            }
            is SettingsState.Event.DetailsButtonMoved -> viewModelScope.launch {
                trackSettingChange(
                    setting = "details_button_order",
                    value = "${event.action.name.lowercase()}_${event.direction.name.lowercase()}",
                )
                settingsStore.setDetailsButtonOrder(
                    currentState.detailsButtonOrder.moved(event.action, event.direction),
                )
            }
            SettingsState.Event.DetailsButtonOrderSelected -> {
                analyticsTracker.track(
                    AnalyticsEvents.uiAction(
                        screenName = SCREEN_NAME,
                        action = "details_button_order_selected",
                    )
                )
                nav.navigate(SettingsDetailsButtonOrderDestination)
            }
            SettingsState.Event.DetailsButtonOrderReset -> viewModelScope.launch {
                trackSettingChange("details_button_order", "reset")
                settingsStore.setDetailsButtonOrder(SettingsStore.defaultDetailsButtonOrder)
            }
        }
    }

    private fun trackSettingChange(setting: String, value: String) {
        analyticsTracker.track(AnalyticsEvents.settingChange(setting, value))
    }
}

private const val SCREEN_NAME = "settings"
