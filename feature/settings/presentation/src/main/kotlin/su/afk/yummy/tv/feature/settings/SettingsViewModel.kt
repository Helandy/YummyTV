package su.afk.yummy.tv.feature.settings

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.auth.TokenStorageMode
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceModePreferences
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.tv.api.ITvIntegration
import su.afk.yummy.tv.core.utils.system.CacheStorageInspector
import su.afk.yummy.tv.domain.videodownload.usecase.ObserveVideoExportDestinationUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.SelectVideoExportDestinationUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.settings.navigator.SettingsDetailsButtonOrderDestination
import su.afk.yummy.tv.feature.settings.utils.moved
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject internal constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val settingsStore: SettingsStore,
    private val yaniAuthPreferences: YaniAuthPreferences,
    private val interfaceModePreferences: AppInterfaceModePreferences,
    private val tvIntegration: ITvIntegration,
    private val nav: INavigationManager,
    private val accountNavigator: IAccountNavigator,
    private val analytics: SettingsAnalytics,
    private val observeVideoExportDestination: ObserveVideoExportDestinationUseCase,
    private val selectVideoExportDestination: SelectVideoExportDestinationUseCase,
    private val cacheStorageInspector: CacheStorageInspector,
) : BaseViewModel<SettingsState.State, SettingsState.Event, SettingsState.Effect>() {

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
                        backgroundStyle = snapshot.backgroundStyle,
                        posterQuality = snapshot.posterQuality,
                        posterCardSize = snapshot.posterCardSize,
                        showTopTitleYear = snapshot.showTopTitleYear,
                        showLibraryTitleYear = snapshot.showLibraryTitleYear,
                        libraryContinueWatchingCardSize = snapshot.libraryContinueWatchingCardSize,
                        preferredPlayer = snapshot.preferredPlayer,
                        preferredVideoQuality = snapshot.preferredVideoQuality,
                        watchNextEnabled = snapshot.watchNextEnabled,
                        previewCacheSize = snapshot.previewCacheSize,
                        autoSkipOpeningsEndings = snapshot.autoSkipOpeningsEndings,
                        autoSkipDelaySeconds = snapshot.autoSkipDelaySeconds,
                        showOpeningOnTimeline = snapshot.showOpeningOnTimeline,
                        autoPlayNextEpisode = snapshot.autoPlayNextEpisode,
                        nextEpisodeSwitchDelaySeconds = snapshot.nextEpisodeSwitchDelaySeconds,
                        askDubbingOnWatch = snapshot.askDubbingOnWatch,
                        pictureInPictureEnabled = snapshot.pictureInPictureEnabled,
                        playerOrientationMode = snapshot.playerOrientationMode,
                        suggestNextEpisodeOnWatched = snapshot.suggestNextEpisodeOnWatched,
                        watchedThresholds = snapshot.watchedThresholds,
                        refreshContinueWatchingProgressOnLaunch =
                            snapshot.refreshContinueWatchingProgressOnLaunch,
                        tvPlayerVolumeKeysEnabled = snapshot.tvPlayerVolumeKeysEnabled,
                        advancedPlayerVolumeEnabled = snapshot.advancedPlayerVolumeEnabled,
                        volumeStabilizationEnabled = snapshot.volumeStabilizationEnabled,
                        playerBufferProfile = snapshot.playerBufferProfile,
                        videoExportAutoEnabled = snapshot.videoExportAutoEnabled,
                        yaniApplicationToken = snapshot.yaniApplicationToken,
                        contentLanguage = snapshot.contentLanguage,
                        detailsButtonOrder = snapshot.detailsButtonOrder.toImmutableList(),
                    )
                }
            }
            .launchIn(viewModelScope)
        settingsStore.playerSubtitleStyle
            .onEach { style -> setState { copy(subtitleStyle = style) } }
            .launchIn(viewModelScope)
        settingsStore.mobilePlayerGestureTutorialDismissed
            .onEach { dismissed ->
                setState { copy(mobilePlayerGestureTutorialDismissed = dismissed) }
            }
            .launchIn(viewModelScope)
        settingsStore.tvPlayerControlsTutorialDismissed
            .onEach { dismissed ->
                setState { copy(tvPlayerControlsTutorialDismissed = dismissed) }
            }
            .launchIn(viewModelScope)
        // Запасной режим хранения сессии показываем в "О приложении": пользователю со сломанным
        // Keystore на кастомной прошивке иначе нечего сообщить в поддержку.
        yaniAuthPreferences.storageMode
            .onEach { mode ->
                setState { copy(isFallbackSessionStorage = mode == TokenStorageMode.FALLBACK) }
            }
            .launchIn(viewModelScope)
        settingsStore.saveLastSearchEnabled
            .onEach { enabled -> setState { copy(saveLastSearchEnabled = enabled) } }
            .launchIn(viewModelScope)
        tvIntegration.previewChannelBrowsable
            .onEach { setState { copy(isPreviewChannelBrowsable = it) } }
            .launchIn(viewModelScope)
        observeVideoExportDestination()
            .onEach { destination ->
                setState { copy(videoExportDirectoryName = destination?.displayName) }
            }
            .launchIn(viewModelScope)
        loadCacheStorage()
    }

    private fun loadCacheStorage() {
        setState { copy(isCacheStorageLoading = true) }
        viewModelScope.launch {
            val report = runCatching { cacheStorageInspector.inspect() }.getOrNull()
            setState {
                copy(
                    isCacheStorageLoading = false,
                    cacheStorageEntries = report?.entries?.toImmutableList() ?: cacheStorageEntries,
                    cacheStorageTotalBytes = report?.totalBytes ?: cacheStorageTotalBytes,
                )
            }
        }
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

            is SettingsState.Event.BackgroundStyleSelected -> viewModelScope.launch {
                analytics.eventBackgroundStyleSelected(event.style)
                settingsStore.setBackgroundStyle(event.style)
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

            SettingsState.Event.ShowLibraryTitleYearToggled -> viewModelScope.launch {
                val enabled = !currentState.showLibraryTitleYear
                analytics.eventShowLibraryTitleYearToggled(enabled)
                settingsStore.setShowLibraryTitleYear(enabled)
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

            is SettingsState.Event.PlayerBufferProfileSelected -> viewModelScope.launch {
                analytics.eventPlayerBufferProfileSelected(event.profile)
                settingsStore.setPlayerBufferProfile(event.profile)
            }

            is SettingsState.Event.SubtitleStyleSelected -> viewModelScope.launch {
                settingsStore.setPlayerSubtitleStyle(event.settings)
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

            is SettingsState.Event.AutoSkipDelayChanged -> viewModelScope.launch {
                analytics.eventAutoSkipDelayChanged(event.seconds)
                settingsStore.setAutoSkipDelaySeconds(event.seconds)
            }

            SettingsState.Event.ShowOpeningOnTimelineToggled -> viewModelScope.launch {
                settingsStore.setShowOpeningOnTimeline(!currentState.showOpeningOnTimeline)
            }

            SettingsState.Event.AutoPlayNextEpisodeToggled -> viewModelScope.launch {
                val enabled = !currentState.autoPlayNextEpisode
                analytics.eventAutoPlayNextEpisodeToggled(enabled)
                settingsStore.setAutoPlayNextEpisode(enabled)
            }

            is SettingsState.Event.WatchedThresholdsChanged -> viewModelScope.launch {
                analytics.eventWatchedThresholdsChanged(event.thresholds)
                settingsStore.setWatchedThresholds(event.thresholds)
            }

            is SettingsState.Event.NextEpisodeSwitchDelayChanged -> viewModelScope.launch {
                analytics.eventNextEpisodeSwitchDelayChanged(event.seconds)
                settingsStore.setNextEpisodeSwitchDelaySeconds(event.seconds)
            }

            SettingsState.Event.AskDubbingOnWatchToggled -> viewModelScope.launch {
                val enabled = !currentState.askDubbingOnWatch
                analytics.eventAskDubbingOnWatchToggled(enabled)
                settingsStore.setAskDubbingOnWatch(enabled)
            }

            SettingsState.Event.PictureInPictureToggled -> viewModelScope.launch {
                val enabled = !currentState.pictureInPictureEnabled
                analytics.eventPictureInPictureToggled(enabled)
                settingsStore.setPictureInPictureEnabled(enabled)
            }

            is SettingsState.Event.PlayerOrientationModeSelected -> viewModelScope.launch {
                analytics.eventPlayerOrientationModeSelected(event.mode)
                settingsStore.setPlayerOrientationMode(event.mode)
            }

            SettingsState.Event.TvPlayerVolumeKeysToggled -> viewModelScope.launch {
                val enabled = !currentState.tvPlayerVolumeKeysEnabled
                analytics.eventTvPlayerVolumeKeysToggled(enabled)
                settingsStore.setTvPlayerVolumeKeysEnabled(enabled)
            }

            SettingsState.Event.AdvancedPlayerVolumeToggled -> viewModelScope.launch {
                val enabled = !currentState.advancedPlayerVolumeEnabled
                analytics.eventAdvancedPlayerVolumeToggled(enabled)
                settingsStore.setAdvancedPlayerVolumeEnabled(enabled)
            }

            SettingsState.Event.VolumeStabilizationToggled -> viewModelScope.launch {
                settingsStore.setVolumeStabilizationEnabled(!currentState.volumeStabilizationEnabled)
            }

            SettingsState.Event.MobilePlayerGestureTutorialReset -> viewModelScope.launch {
                analytics.eventMobilePlayerGestureTutorialReset()
                settingsStore.resetMobilePlayerGestureTutorial()
            }

            SettingsState.Event.TvPlayerControlsTutorialReset -> viewModelScope.launch {
                analytics.eventTvPlayerControlsTutorialReset()
                settingsStore.resetTvPlayerControlsTutorial()
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

            SettingsState.Event.LoginOnTvSelected -> {
                nav.navigate(accountNavigator.getLocalAuthDest())
            }

            SettingsState.Event.DetailsButtonOrderReset -> viewModelScope.launch {
                analytics.eventDetailsButtonOrderReset()
                settingsStore.setDetailsButtonOrder(SettingsStore.defaultDetailsButtonOrder)
            }

            SettingsState.Event.VideoExportDirectorySelected -> {
                setEffect(SettingsState.Effect.OpenVideoExportDirectoryPicker)
            }

            SettingsState.Event.VideoExportAutoToggled -> viewModelScope.launch {
                val enabled = !currentState.videoExportAutoEnabled
                analytics.eventVideoExportAutoToggled(enabled)
                settingsStore.setVideoExportAutoEnabled(enabled)
            }

            is SettingsState.Event.VideoExportDirectoryGranted -> viewModelScope.launch {
                runCatching { selectVideoExportDestination(event.uri) }
                    .onFailure {
                        setEffect(SettingsState.Effect.VideoExportDirectorySelectionFailed)
                    }
            }

            SettingsState.Event.CacheStorageRefreshRequested -> loadCacheStorage()

            SettingsState.Event.SaveLastSearchToggled -> viewModelScope.launch {
                val enabled = !currentState.saveLastSearchEnabled
                analytics.eventSaveLastSearchToggled(enabled)
                settingsStore.setSaveLastSearchEnabled(enabled)
            }
        }
    }
}
