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
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceModePreferences
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.tv.api.ITvIntegration
import su.afk.yummy.tv.domain.videodownload.usecase.ObserveVideoExportDestinationUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.SelectVideoExportDestinationUseCase
import su.afk.yummy.tv.feature.settings.navigator.SettingsDetailsButtonOrderDestination
import su.afk.yummy.tv.feature.settings.utils.moved
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject internal constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val settingsStore: SettingsStore,
    private val interfaceModePreferences: AppInterfaceModePreferences,
    private val tvIntegration: ITvIntegration,
    private val nav: NavigationManager,
    private val analytics: SettingsAnalytics,
    private val observeVideoExportDestination: ObserveVideoExportDestinationUseCase,
    private val selectVideoExportDestination: SelectVideoExportDestinationUseCase,
) : BaseViewModelNew<SettingsState.State, SettingsState.Event, SettingsState.Effect>(
    savedStateHandle
) {

    override fun createInitialState() = SettingsState.State(
        interfaceMode = interfaceModePreferences.selectedMode ?: AppInterfaceMode.MOBILE,
    )

    init {
        analytics.eventScreenOpened()
        settingsStore.settingsSnapshot
            .onEach { snapshot ->
                setState {
                    copy(
                        appTheme = snapshot.appTheme,
                        posterQuality = snapshot.posterQuality,
                        posterCardSize = snapshot.posterCardSize,
                        showTopTitleYear = snapshot.showTopTitleYear,
                        libraryContinueWatchingCardSize = snapshot.libraryContinueWatchingCardSize,
                        preferredPlayer = snapshot.preferredPlayer,
                        preferredVideoQuality = snapshot.preferredVideoQuality,
                        watchNextEnabled = snapshot.watchNextEnabled,
                        previewCacheSize = snapshot.previewCacheSize,
                        autoSkipOpeningsEndings = snapshot.autoSkipOpeningsEndings,
                        autoPlayNextEpisode = snapshot.autoPlayNextEpisode,
                        pictureInPictureEnabled = snapshot.pictureInPictureEnabled,
                        suggestNextEpisodeOnWatched = snapshot.suggestNextEpisodeOnWatched,
                        refreshContinueWatchingProgressOnLaunch =
                            snapshot.refreshContinueWatchingProgressOnLaunch,
                        tvPlayerVolumeKeysEnabled = snapshot.tvPlayerVolumeKeysEnabled,
                        videoExportAutoEnabled = snapshot.videoExportAutoEnabled,
                        yaniApplicationToken = snapshot.yaniApplicationToken,
                        contentLanguage = snapshot.contentLanguage,
                        detailsButtonOrder = snapshot.detailsButtonOrder,
                    )
                }
            }
            .launchIn(viewModelScope)
        settingsStore.mobilePlayerGestureTutorialDismissed
            .onEach { dismissed ->
                setState { copy(mobilePlayerGestureTutorialDismissed = dismissed) }
            }
            .launchIn(viewModelScope)
        tvIntegration.previewChannelBrowsable
            .onEach { setState { copy(isPreviewChannelBrowsable = it) } }
            .launchIn(viewModelScope)
        observeVideoExportDestination()
            .onEach { destination ->
                setState { copy(videoExportDirectoryName = destination?.displayName) }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: SettingsState.Event) {
        when (event) {
            SettingsState.Event.BackSelected -> nav.back()
            is SettingsState.Event.InterfaceModeSelected -> {
                if (event.mode == currentState.interfaceMode) return
                interfaceModePreferences.select(event.mode)
                analytics.eventInterfaceModeSelected(event.mode)
                setState { copy(interfaceMode = event.mode) }
                setEffect(SettingsState.Effect.RestartApplication)
            }

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

            SettingsState.Event.ShowTopTitleYearToggled -> viewModelScope.launch {
                val enabled = !currentState.showTopTitleYear
                analytics.eventShowTopTitleYearToggled(enabled)
                settingsStore.setShowTopTitleYear(enabled)
            }

            is SettingsState.Event.LibraryContinueWatchingCardSizeSelected ->
                viewModelScope.launch {
                    analytics.eventLibraryContinueWatchingCardSizeSelected(event.size)
                    settingsStore.setLibraryContinueWatchingCardSize(event.size)
                }

            is SettingsState.Event.PreferredPlayerSelected -> viewModelScope.launch {
                analytics.eventPreferredPlayerSelected(event.player)
                settingsStore.setPreferredPlayer(event.player)
            }

            is SettingsState.Event.PreferredVideoQualitySelected -> viewModelScope.launch {
                analytics.eventPreferredVideoQualitySelected(event.quality)
                settingsStore.setPreferredVideoQuality(event.quality)
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

            SettingsState.Event.AutoPlayNextEpisodeToggled -> viewModelScope.launch {
                val enabled = !currentState.autoPlayNextEpisode
                analytics.eventAutoPlayNextEpisodeToggled(enabled)
                settingsStore.setAutoPlayNextEpisode(enabled)
            }

            SettingsState.Event.PictureInPictureToggled -> viewModelScope.launch {
                val enabled = !currentState.pictureInPictureEnabled
                analytics.eventPictureInPictureToggled(enabled)
                settingsStore.setPictureInPictureEnabled(enabled)
            }

            SettingsState.Event.TvPlayerVolumeKeysToggled -> viewModelScope.launch {
                settingsStore.setTvPlayerVolumeKeysEnabled(!currentState.tvPlayerVolumeKeysEnabled)
            }

            SettingsState.Event.MobilePlayerGestureTutorialReset -> viewModelScope.launch {
                analytics.eventMobilePlayerGestureTutorialReset()
                settingsStore.resetMobilePlayerGestureTutorial()
            }

            SettingsState.Event.SuggestNextEpisodeOnWatchedToggled -> viewModelScope.launch {
                val enabled = !currentState.suggestNextEpisodeOnWatched
                analytics.eventSuggestNextEpisodeOnWatchedToggled(enabled)
                settingsStore.setSuggestNextEpisodeOnWatched(enabled)
            }

            SettingsState.Event.RefreshContinueWatchingProgressOnLaunchToggled ->
                viewModelScope.launch {
                    val enabled = !currentState.refreshContinueWatchingProgressOnLaunch
                    analytics.eventRefreshContinueWatchingProgressOnLaunchToggled(enabled)
                    settingsStore.setRefreshContinueWatchingProgressOnLaunch(enabled)
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

            SettingsState.Event.VideoExportDirectorySelected -> {
                setEffect(SettingsState.Effect.OpenVideoExportDirectoryPicker)
            }

            SettingsState.Event.VideoExportAutoToggled -> viewModelScope.launch {
                settingsStore.setVideoExportAutoEnabled(!currentState.videoExportAutoEnabled)
            }

            is SettingsState.Event.VideoExportDirectoryGranted -> viewModelScope.launch {
                runCatching { selectVideoExportDestination(event.uri) }
                    .onFailure {
                        setEffect(SettingsState.Effect.VideoExportDirectorySelectionFailed)
                    }
            }
        }
    }
}
