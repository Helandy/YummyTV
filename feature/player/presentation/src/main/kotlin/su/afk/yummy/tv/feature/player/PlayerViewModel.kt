package su.afk.yummy.tv.feature.player

import android.util.Log
import androidx.navigation3.runtime.NavKey
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.settings.PlayerMobileVideoTransformSettings
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.model.settings.PlayerResizeSettings
import su.afk.yummy.tv.core.model.settings.PlayerZoomLevel
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.coroutines.di.IoApplicationScope
import su.afk.yummy.tv.domain.player.model.AllohaAudioTrack
import su.afk.yummy.tv.domain.player.model.AllohaSubtitleTrack
import su.afk.yummy.tv.domain.videodownload.usecase.GetVideoDownloadUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.player.PlayerViewModel.Companion.CHANGE_PLAYER_HINT_DELAY_MS
import su.afk.yummy.tv.feature.player.handler.PlayerAllohaRecoveryHandler
import su.afk.yummy.tv.feature.player.handler.PlayerAllohaSessionHandler
import su.afk.yummy.tv.feature.player.handler.PlayerAllohaTrackPreferenceHandler
import su.afk.yummy.tv.feature.player.handler.PlayerDisplaySettingsHandler
import su.afk.yummy.tv.feature.player.handler.PlayerFinalEpisodeActionHandler
import su.afk.yummy.tv.feature.player.handler.PlayerPlaybackProgressHandler
import su.afk.yummy.tv.feature.player.handler.PlayerPlaybackRetryHandler
import su.afk.yummy.tv.feature.player.handler.PlayerSettingsHandler
import su.afk.yummy.tv.feature.player.handler.PlayerSourceGraphLoadResult
import su.afk.yummy.tv.feature.player.handler.PlayerSourceSelectionHandler
import su.afk.yummy.tv.feature.player.handler.PlayerSourceStreamHandler
import su.afk.yummy.tv.feature.player.handler.PlayerStreamLoadResult
import su.afk.yummy.tv.feature.player.handler.PlayerStreamResumeMode
import su.afk.yummy.tv.feature.player.mapper.PlayerDestinationStateMapper
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction
import su.afk.yummy.tv.feature.player.navigator.PLAYER_CONTENT_KEY
import su.afk.yummy.tv.feature.player.navigator.PlayerDestination
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.utils.PlayerResizeSettingsScope
import su.afk.yummy.tv.feature.player.utils.activeBalancerName
import su.afk.yummy.tv.feature.player.utils.activeDubbingName
import su.afk.yummy.tv.feature.player.utils.activeEpisode
import su.afk.yummy.tv.feature.player.utils.activeIframeUrl
import su.afk.yummy.tv.feature.player.utils.activePlayerId
import su.afk.yummy.tv.feature.player.utils.activeScreenshotUrl
import su.afk.yummy.tv.feature.player.utils.activeVideoId

@HiltViewModel(assistedFactory = PlayerViewModel.Factory::class)
class PlayerViewModel @AssistedInject internal constructor(
    @Assisted private val dest: PlayerDestination,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val sourceStreamHandler: PlayerSourceStreamHandler,
    private val playbackProgressHandler: PlayerPlaybackProgressHandler,
    private val settingsHandler: PlayerSettingsHandler,
    private val displaySettingsHandler: PlayerDisplaySettingsHandler,
    private val finalEpisodeActionHandler: PlayerFinalEpisodeActionHandler,
    private val destinationStateMapper: PlayerDestinationStateMapper,
    private val sourceSelectionHandler: PlayerSourceSelectionHandler,
    private val getVideoDownload: GetVideoDownloadUseCase,
    private val strings: StringProvider,
    private val analytics: PlayerAnalytics,
    private val allohaRecovery: PlayerAllohaRecoveryHandler,
    private val playbackRetry: PlayerPlaybackRetryHandler,
    private val allohaSession: PlayerAllohaSessionHandler,
    private val allohaTrackPreference: PlayerAllohaTrackPreferenceHandler,
    @IoApplicationScope private val ioScope: CoroutineScope,
) : BaseViewModel<PlayerState.State, PlayerState.Event, PlayerState.Effect>() {

    @AssistedFactory
    interface Factory {
        fun create(dest: PlayerDestination): PlayerViewModel
    }

    private var activeDest: PlayerDestination = dest
    private var pendingDestinationResumeMs: Long? = dest.resumeFromMs.takeIf { it > 0L }
    private var sourceGraphJob: Job? = null
    private var allohaPlaybackRecoveryJob: Job? = null
    private var playbackRetryJob: Job? = null
    private var streamLoadingHintJob: Job? = null
    private var finalEpisodeActionJob: Job? = null
    private var mobileGestureTutorialReady = false
    private var showMobileGestureTutorial = false
    private var tvControlsTutorialReady = false
    private var showTvControlsTutorial = false
    private var isNavigatingToChildScreen = false

    private fun loadDestination(newDest: PlayerDestination) {
        if (newDest == activeDest) return
        allohaSession.close()
        allohaRecovery.reset()
        allohaPlaybackRecoveryJob?.cancel()
        playbackRetry.reset()
        playbackRetryJob?.cancel()
        activeDest = newDest
        pendingDestinationResumeMs = newDest.resumeFromMs.takeIf { it > 0L }
        setState {
            destinationStateMapper.toState(
                newDest,
                autoSkipOpeningsEndings = autoSkipOpeningsEndings,
                autoPlayNextEpisode = autoPlayNextEpisode,
                nextEpisodeSwitchDelaySeconds = nextEpisodeSwitchDelaySeconds,
                pictureInPictureEnabled = pictureInPictureEnabled,
            ).copy(
                playerOrientationMode = playerOrientationMode,
                mobileGestureTutorialReady = mobileGestureTutorialReady,
                showMobileGestureTutorial = showMobileGestureTutorial,
                tvControlsTutorialReady = tvControlsTutorialReady,
                showTvControlsTutorial = showTvControlsTutorial,
                tvPlayerVolumeKeysEnabled = tvPlayerVolumeKeysEnabled,
                advancedPlayerVolumeEnabled = advancedPlayerVolumeEnabled,
                showOpeningOnTimeline = showOpeningOnTimeline,
            )
        }
        loadFinalEpisodeAction(newDest.animeId)
        observeActivePlayerResizeSettings(force = true)
        observeActivePlayerMobileVideoTransformSettings(force = true)
        if (newDest.downloadId > 0L) {
            loadDownloadedDestination(newDest.downloadId)
        } else if (newDest.localFileUri.isNotBlank()) {
            loadLocalFileDestination(newDest.localFileUri, newDest.animeTitle)
        } else {
            loadSourceGraph()
            loadStream()
        }
    }

    override fun createInitialState() = destinationStateMapper.toState(dest)

    private var extractionJob: Job? = null

    init {
        analytics.eventScreenOpened(dest.animeId)
        settingsHandler.autoSkipOpeningsEndings
            .onEach { enabled -> setState { copy(autoSkipOpeningsEndings = enabled) } }
            .launchIn(viewModelScope)
        settingsHandler.showOpeningOnTimeline
            .onEach { enabled -> setState { copy(showOpeningOnTimeline = enabled) } }
            .launchIn(viewModelScope)
        settingsHandler.autoPlayNextEpisode
            .onEach { enabled -> setState { copy(autoPlayNextEpisode = enabled) } }
            .launchIn(viewModelScope)
        settingsHandler.nextEpisodeSwitchDelaySeconds
            .onEach { seconds -> setState { copy(nextEpisodeSwitchDelaySeconds = seconds) } }
            .launchIn(viewModelScope)
        settingsHandler.pictureInPictureEnabled
            .onEach { enabled -> setState { copy(pictureInPictureEnabled = enabled) } }
            .launchIn(viewModelScope)
        settingsHandler.playerOrientationMode
            .onEach { mode -> setState { copy(playerOrientationMode = mode) } }
            .launchIn(viewModelScope)
        settingsHandler.mobilePlayerGestureTutorialDismissed
            .onEach { dismissed ->
                mobileGestureTutorialReady = true
                showMobileGestureTutorial = !dismissed
                setState {
                    copy(
                        mobileGestureTutorialReady = true,
                        showMobileGestureTutorial = !dismissed,
                    )
                }
            }
            .launchIn(viewModelScope)
        settingsHandler.tvPlayerControlsTutorialDismissed
            .onEach { dismissed ->
                tvControlsTutorialReady = true
                showTvControlsTutorial = !dismissed
                setState {
                    copy(
                        tvControlsTutorialReady = true,
                        showTvControlsTutorial = !dismissed,
                    )
                }
            }
            .launchIn(viewModelScope)
        settingsHandler.tvPlayerVolumeKeysEnabled
            .onEach { enabled -> setState { copy(tvPlayerVolumeKeysEnabled = enabled) } }
            .launchIn(viewModelScope)
        settingsHandler.advancedPlayerVolumeEnabled
            .onEach { enabled -> setState { copy(advancedPlayerVolumeEnabled = enabled) } }
            .launchIn(viewModelScope)
        settingsHandler.playerSubtitleStyle
            .onEach { style -> setState { copy(subtitleStyle = style) } }
            .launchIn(viewModelScope)
        if (dest.downloadId > 0L) {
            loadDownloadedDestination(dest.downloadId)
        } else if (dest.localFileUri.isNotBlank()) {
            observeActivePlayerResizeSettings()
            observeActivePlayerMobileVideoTransformSettings()
            loadLocalFileDestination(dest.localFileUri, dest.animeTitle)
        } else {
            loadFinalEpisodeAction(dest.animeId)
            observeActivePlayerResizeSettings()
            observeActivePlayerMobileVideoTransformSettings()
            loadSourceGraph()
            loadStream()
        }
    }

    override fun onEvent(event: PlayerState.Event) {
        when (event) {
            is PlayerState.Event.NavigateToDestination -> loadDestination(event.destination)

            PlayerState.Event.Back -> saveCurrentProgressThenNavigate {
                allohaSession.close()
                nav.back()
            }

            PlayerState.Event.MobileGestureTutorialDismissed -> {
                showMobileGestureTutorial = false
                setState { copy(showMobileGestureTutorial = false) }
                viewModelScope.launch {
                    settingsHandler.dismissMobilePlayerGestureTutorial()
                }
            }

            PlayerState.Event.TvControlsTutorialDismissed -> {
                showTvControlsTutorial = false
                setState { copy(showTvControlsTutorial = false) }
                viewModelScope.launch {
                    settingsHandler.dismissTvPlayerControlsTutorial()
                }
            }

            PlayerState.Event.OpenDetails -> {
                analytics.eventOpenDetails(currentState.animeId)
                val animeId = currentState.animeId
                if (animeId > 0) {
                    saveCurrentProgressThenNavigate {
                        nav.navigate(detailsNavigator.getDetailsDest(animeId))
                    }
                }
            }

            PlayerState.Event.RetryStream -> {
                analytics.eventRetryStream(currentState.animeId)
                playbackRetry.reset()
                playbackRetryJob?.cancel()
                if (currentState.isLocalFile) {
                    setState { copy(retryKey = retryKey + 1) }
                    activeDest.localFileUri.takeIf(String::isNotBlank)?.let {
                        loadLocalFileDestination(it, activeDest.animeTitle)
                    }
                } else if (currentState.isOfflinePlayback) {
                    setState { copy(retryKey = retryKey + 1) }
                    loadDownloadedDestination(activeDest.downloadId)
                } else if (currentState.isAllohaSource()) {
                    startAllohaPlaybackRecovery(
                        positionMs = currentState.playbackPositionMs
                            .takeIf { it > 0L }
                            ?: currentState.resumeFromMs,
                        selectedQuality = currentState.selectedQuality,
                        initialDelayMs = 0L,
                    )
                } else {
                    setState { copy(retryKey = retryKey + 1) }
                    allohaSession.close()
                    loadStream(refreshSourcesOnFailure = true, forceRefresh = true)
                }
            }

            PlayerState.Event.TvAppBackgrounded -> {
                if (!isNavigatingToChildScreen) returnToDetailsAfterTvBackground()
            }

            PlayerState.Event.RateTitle -> {
                analytics.eventRateTitle(currentState.animeId)
                val animeId = currentState.animeId
                if (animeId > 0 && !isNavigatingToChildScreen) {
                    isNavigatingToChildScreen = true
                    saveCurrentProgressThenNavigate {
                        navigateFromPlayerToChild(
                            animeId = animeId,
                            childDestination = detailsNavigator.getRatingDest(animeId),
                        )
                    }
                }
            }

            PlayerState.Event.ManageSubscriptions -> {
                analytics.eventManageSubscriptions(currentState.animeId)
                val animeId = currentState.animeId
                if (animeId > 0 && !isNavigatingToChildScreen) {
                    isNavigatingToChildScreen = true
                    saveCurrentProgressThenNavigate {
                        navigateFromPlayerToChild(
                            animeId = animeId,
                            childDestination = detailsNavigator.getSubscriptionsDest(animeId),
                        )
                    }
                }
            }

            is PlayerState.Event.PlaybackError -> {
                if (!currentState.isOfflinePlayback && currentState.isAllohaSource()) {
                    if (allohaRecovery.isRecovering) {
                        Log.w(
                            LOG_TAG,
                            "Ignoring duplicate Alloha playback error during fresh-session recovery " +
                                    "positionMs=${event.positionMs.coerceAtLeast(0L)}",
                        )
                        return
                    }
                    startAllohaPlaybackRecovery(
                        positionMs = event.positionMs,
                        selectedQuality = currentState.selectedQuality,
                        initialDelayMs = ALLOHA_PLAYBACK_RECOVERY_DELAY_MS,
                    )
                } else if (!currentState.isOfflinePlayback && playbackRetry.canRetry()) {
                    schedulePlaybackRetryAttempt()
                } else {
                    // Тихие ретраи исчерпаны (или offline) — сейчас юзеру показывается окно
                    // «повторить/сменить». Логируем именно здесь, а не на каждую ошибку плеера,
                    // чтобы не репортить транзиентные сбои, которые сами починились ретраем.
                    analytics.eventPlaybackError(
                        state = currentState,
                        message = event.message,
                        errorCode = event.errorCode,
                        errorType = event.errorType,
                        retryAttempts = playbackRetry.attempts,
                    )
                    playbackRetry.reset()
                    playbackRetryJob?.cancel()
                    streamLoadingHintJob?.cancel()
                    setState {
                        copy(
                            streamUrl = null,
                            isPlaybackRecovering = false,
                            playerError = sourceStreamHandler.playbackErrorMessage(
                                message = event.message,
                                errorCode = event.errorCode,
                            ),
                            showChangePlayerHint = false,
                        )
                    }
                }
            }

            PlayerState.Event.PlaybackReady -> {
                // Успешный старт - бюджет тихих повторов освобождается для нового сеанса.
                playbackRetry.reset()
                if (
                    currentState.isPlaybackRecovering &&
                    !allohaRecovery.isRecovering &&
                    !currentState.isOfflinePlayback &&
                    currentState.isAllohaSource()
                ) {
                    Log.i(
                        LOG_TAG,
                        "Background Alloha playback recovery ready " +
                                "positionMs=${currentState.playbackPositionMs.coerceAtLeast(0L)}",
                    )
                    setState { copy(isPlaybackRecovering = false) }
                } else if (currentState.isPlaybackRecovering && !allohaRecovery.isRecovering) {
                    Log.i(
                        LOG_TAG,
                        "Silent playback retry recovered " +
                                "positionMs=${currentState.playbackPositionMs.coerceAtLeast(0L)}",
                    )
                    setState { copy(isPlaybackRecovering = false) }
                }
            }

            PlayerState.Event.PrevEpisode -> {
                allohaSession.close()
                analytics.eventPrevEpisode(currentState.animeId)
                applySourceSelection(
                    sourceSelectionHandler.previousEpisode(currentState),
                    resumeMode = PlayerStreamResumeMode.SelectedSourceOnly,
                    refreshSourcesBeforeStream = true,
                )
            }

            is PlayerState.Event.NextEpisode -> {
                allohaSession.close()
                analytics.eventNextEpisode(currentState, event.source)
                val nextState = sourceSelectionHandler.nextEpisode(currentState)
                    ?: sourceSelectionHandler.nextEpisodeInOtherDubbing(currentState)
                applySourceSelection(
                    nextState,
                    sourceScopeChanged = true,
                    resumeMode = PlayerStreamResumeMode.SelectedSourceOnly,
                    refreshSourcesBeforeStream = true,
                )
                nextState?.let(::saveContinueTarget)
            }

            is PlayerState.Event.EpisodeCompleted -> {
                if (!isActivePlaybackSource(event.episodeUrl)) return
                playbackProgressHandler.reportEpisodeFullyCompleted(
                    state = currentState,
                    positionMs = event.positionMs,
                    durationMs = event.durationMs,
                )
                saveWatchedProgressIfNeeded(event.positionMs, event.durationMs)
            }

            is PlayerState.Event.DubbingSelected -> {
                allohaSession.close()
                analytics.eventDubbingSelected(
                    state = currentState,
                    index = event.index,
                    positionMs = event.currentPosMs,
                )
                applySourceSelection(
                    sourceSelectionHandler.selectDubbing(
                        state = currentState,
                        index = event.index,
                        currentPosMs = event.currentPosMs,
                    ),
                    sourceScopeChanged = true,
                )
            }

            is PlayerState.Event.BalancerSelected -> {
                allohaSession.close()
                analytics.eventBalancerSelected(
                    state = currentState,
                    index = event.index,
                    positionMs = event.currentPosMs,
                )
                applySourceSelection(
                    sourceSelectionHandler.selectBalancer(
                        state = currentState,
                        index = event.index,
                        currentPosMs = event.currentPosMs,
                    ),
                    sourceScopeChanged = true,
                )
            }

            is PlayerState.Event.QualitySelected -> {
                analytics.eventQualitySelected(currentState.animeId, event.quality)
                val position = event.currentPosMs.coerceAtLeast(0L)
                if (allohaRecovery.isRecovering) {
                    allohaRecovery.selectedQuality = event.quality
                }
                allohaSession.selectQuality(event.quality)
                setState {
                    copy(
                        selectedQuality = event.quality,
                        resumeFromMs = position,
                        playbackPositionMs = position,
                    )
                }
            }

            is PlayerState.Event.AllohaAudioTrackSelected -> {
                val position = event.currentPosMs.coerceAtLeast(0L)
                // Same live session, different stream URL - no re-extraction, like quality.
                val stream = allohaSession.selectAudioTrack(event.audioId) ?: return
                setState {
                    copy(
                        selectedAllohaAudioId = stream.selectedAllohaAudioId,
                        streamQualityMap = stream.qualities,
                        selectedQuality = selectedQuality?.takeIf {
                            stream.qualities?.containsKey(it) == true
                        },
                        streamUrl = stream.url,
                        resumeFromMs = position,
                        playbackPositionMs = position,
                    )
                }
                saveAllohaAudioPreference(event.audioId)
            }

            is PlayerState.Event.AllohaSubtitleSelected -> {
                val index = event.index?.takeIf { it in currentState.allohaSubtitles.indices }
                setState {
                    copy(selectedAllohaSubtitleIndex = index)
                }
                saveAllohaSubtitlePreference(index)
            }

            is PlayerState.Event.SpeedSelected -> {
                analytics.eventSpeedSelected(currentState.animeId, event.speed)
                setState { copy(selectedSpeed = event.speed.coerceAtLeast(0.1f)) }
            }

            is PlayerState.Event.ResizeModeSelected -> {
                analytics.eventResizeModeSelected(currentState.animeId, event.mode)
                val settings = PlayerResizeSettings(
                    resizeMode = event.mode,
                    zoomLevel = currentState.zoomLevel,
                )
                setState { copy(resizeMode = settings.resizeMode) }
                savePlayerResizeSettings(settings)
            }

            is PlayerState.Event.ZoomLevelSelected -> {
                analytics.eventZoomLevelSelected(currentState.animeId, event.level)
                val settings = PlayerResizeSettings(
                    resizeMode = PlayerResizeMode.ZOOM,
                    zoomLevel = event.level,
                )
                setState { copy(resizeMode = settings.resizeMode, zoomLevel = settings.zoomLevel) }
                savePlayerResizeSettings(settings)
            }

            is PlayerState.Event.MobileVideoTransformChanged -> {
                val settings = PlayerMobileVideoTransformSettings(
                    scale = event.scale,
                    offsetX = event.offsetX,
                    offsetY = event.offsetY,
                )
                setState {
                    copy(
                        mobileVideoScale = settings.scale,
                        mobileVideoOffsetX = settings.offsetX,
                        mobileVideoOffsetY = settings.offsetY,
                    )
                }
                savePlayerMobileVideoTransformSettings(settings)
            }

            is PlayerState.Event.PlaybackPositionChanged -> {
                if (!isActivePlaybackSource(event.episodeUrl)) return
                val position = event.positionMs.coerceAtLeast(0L)
                val duration = event.durationMs.coerceAtLeast(0L)
                if (allohaRecovery.isRecovering && position > 0L) {
                    allohaRecovery.positionMs = position
                }
                setState {
                    copy(
                        resumeFromMs = position,
                        playbackPositionMs = position,
                        playbackDurationMs = duration,
                    )
                }
                playbackProgressHandler.recordWatchedTick(currentState, position, duration)
                saveWatchedProgressIfNeeded(position, duration)
            }

            is PlayerState.Event.SkipSegmentSelected -> {
                analytics.eventSkipSegmentSelected(
                    state = currentState,
                    type = event.type,
                    fromMs = event.fromMs,
                    toMs = event.toMs,
                )
            }

            is PlayerState.Event.SaveProgress -> {
                val s = currentState
                val snapshot = event.snapshot
                viewModelScope.launch {
                    playbackProgressHandler.saveProgress(
                        playbackProgressHandler.progressSaveRequest(s, snapshot)
                    )
                }
            }

        }
    }

    private fun loadDownloadedDestination(downloadId: Long) {
        allohaSession.close()
        streamLoadingHintJob?.cancel()
        viewModelScope.launch {
            val item = getVideoDownload(downloadId)
            if (item == null || item.status.name != "Downloaded") {
                setState {
                    copy(
                        playerError = strings.get(R.string.player_download_missing),
                        streamUrl = null,
                    )
                }
                return@launch
            }
            val sourceGraph = PlayerSourceGraph(
                balancers = listOf(
                    PlayerSourceBalancer(
                        name = item.playerName,
                        dubbings = listOf(
                            PlayerSourceDubbing(
                                name = item.dubbing,
                                episodes = listOf(
                                    PlayerSourceEpisode(
                                        id = item.videoId,
                                        playerId = item.playerId,
                                        number = item.episode,
                                        iframeUrl = item.iframeUrl,
                                        screenshotUrl = item.screenshotUrl,
                                    )
                                ),
                            )
                        ),
                    )
                ),
            )
            setState {
                copy(
                    animeTitle = item.animeTitle,
                    animeId = item.animeId,
                    posterUrl = item.posterUrl,
                    sourceGraph = sourceGraph,
                    sourceSelection = PlayerSourceSelection(),
                    streamUrl = item.streamUrl,
                    streamHeaders = item.headers,
                    selectedQuality = item.qualityLabel,
                    allohaAudioTracks = emptyList(),
                    selectedAllohaAudioId = null,
                    allohaSubtitles = emptyList(),
                    selectedAllohaSubtitleIndex = null,
                    isOfflinePlayback = true,
                    offlineCacheKey = item.cacheKey,
                    offlineCacheKeyScheme = item.cacheKeyScheme.storageValue,
                    playerError = null,
                )
            }
            loadFinalEpisodeAction(item.animeId)
        }
    }

    /**
     * Воспроизведение локального файла, открытого извне (ACTION_VIEW, content://).
     * Идёт по офлайн-маршруту, чтобы не запускать сетевые ретраи/резолв source-graph,
     * но с отдельным локальным data-source (флаг [PlayerState.State.isLocalFile]).
     */
    private fun loadLocalFileDestination(uri: String, title: String) {
        allohaSession.close()
        streamLoadingHintJob?.cancel()
        val displayTitle = title.takeIf(String::isNotBlank)
            ?: strings.get(R.string.player_local_file_title)
        val sourceGraph = PlayerSourceGraph(
            balancers = listOf(
                PlayerSourceBalancer(
                    name = "",
                    dubbings = listOf(
                        PlayerSourceDubbing(
                            name = "",
                            episodes = listOf(
                                PlayerSourceEpisode(
                                    id = 0,
                                    playerId = null,
                                    number = "",
                                    iframeUrl = uri,
                                    screenshotUrl = "",
                                )
                            ),
                        )
                    ),
                )
            ),
        )
        setState {
            copy(
                animeTitle = displayTitle,
                animeId = 0,
                posterUrl = "",
                sourceGraph = sourceGraph,
                sourceSelection = PlayerSourceSelection(),
                streamUrl = uri,
                streamHeaders = emptyMap(),
                streamQualityMap = null,
                selectedQuality = null,
                allohaAudioTracks = emptyList(),
                selectedAllohaAudioId = null,
                allohaSubtitles = emptyList(),
                selectedAllohaSubtitleIndex = null,
                isOfflinePlayback = true,
                isLocalFile = true,
                offlineCacheKey = null,
                playerError = null,
            )
        }
    }

    private fun loadFinalEpisodeAction(animeId: Int) {
        finalEpisodeActionJob?.cancel()
        setState { copy(finalEpisodeAction = PlayerFinalEpisodeAction.Loading) }
        finalEpisodeActionJob = viewModelScope.launch {
            val action = finalEpisodeActionHandler.resolve(animeId)
            setState { copy(finalEpisodeAction = action) }
        }
    }

    private fun saveCurrentProgressThenNavigate(navigate: () -> Unit) {
        val request = playbackProgressHandler.currentProgressSaveRequest(state = currentState)
        if (request == null) {
            navigate()
            return
        }

        viewModelScope.launch {
            runCatching {
                playbackProgressHandler.saveProgress(request)
            }
            navigate()
        }
    }

    /**
     * Убирает плеер со стека, когда приложение уходит в фон.
     *
     * Навигация выполняется синхронно, прямо в обработке `ON_STOP`: `onSaveInstanceState`
     * вызывается сразу за `onStop`, и отложенная в корутину замена ключа не успевает попасть
     * в сохранённый back stack — после смерти процесса приложение восстанавливалось в плеере
     * на той серии, с которой его когда-то открыли. Сохранение прогресса уезжает в
     * [ioScope]: `nav.replace` уничтожает NavEntry плеера, и `viewModelScope` вместе с ним.
     */
    private fun returnToDetailsAfterTvBackground() {
        val animeId = currentState.animeId
        val request = playbackProgressHandler.currentProgressSaveRequest(state = currentState)

        allohaSession.close()
        if (animeId <= 0) {
            nav.back()
        } else {
            val detailsDestination = detailsNavigator.getDetailsDest(animeId)
            val previousDestination = nav.backStack.getOrNull(nav.backStack.lastIndex - 1)
            if (previousDestination == detailsDestination) {
                nav.back()
            } else {
                nav.replace(detailsDestination)
            }
        }

        if (request == null) return
        ioScope.launch {
            runCatching { playbackProgressHandler.saveProgress(request) }
        }
    }

    private fun navigateFromPlayerToChild(animeId: Int, childDestination: NavKey) {
        val detailsDestination = detailsNavigator.getDetailsDest(animeId)
        val previousDestination = nav.backStack.getOrNull(nav.backStack.lastIndex - 1)
        if (previousDestination == detailsDestination) {
            nav.replace(childDestination)
        } else {
            nav.replace(detailsDestination)
            nav.navigate(childDestination)
        }
    }

    /**
     * Приводит ключ плеера в back stack к серии, которая реально играет.
     *
     * NavKey переживает смерть процесса (`rememberNavBackStack`), а смена серии внутри плеера
     * меняла только state ViewModel — после перезапуска приложение возвращалось на серию, с
     * которой плеер когда-то открыли. Запись делит [PLAYER_CONTENT_KEY] с текущей
     * (см. `PlayerNavRegistrar`), поэтому подмена ключа не пересоздаёт NavEntry и не рвёт
     * воспроизведение.
     */
    private fun syncBackStackDestination() {
        if (activeDest.downloadId > 0L || activeDest.localFileUri.isNotBlank()) return
        val state = currentState
        val iframeUrl = activeIframeUrl(state)
        val episode = activeEpisode(state)
        if (iframeUrl.isBlank() || episode.isBlank()) return

        val destination = activeDest.copy(
            iframeUrl = iframeUrl,
            episode = episode,
            playerName = activeBalancerName(state),
            dubbing = activeDubbingName(state),
            selectedVideoId = activeVideoId(state),
            selectedPlayerId = activePlayerId(state),
            selectedScreenshotUrl = activeScreenshotUrl(state),
            // Позицию не дублируем в ключ: её вернёт локальный прогресс
            // (PlayerStreamHandler.loadResumePosition), иначе ключ пришлось бы переписывать
            // на каждом тике воспроизведения.
            resumeFromMs = 0L,
        )
        if (destination == activeDest) return
        // Ключ и activeDest двигаются только вместе: разъехавшись, они дадут повторный
        // NavigateToDestination со старой серией, если запись пересоздадут.
        if (nav.backStack.lastOrNull() !is PlayerDestination) return
        // activeDest обновляем до replace: экран пришлёт NavigateToDestination с новым ключом,
        // и loadDestination должен схлопнуться на guard, а не перезапустить загрузку.
        activeDest = destination
        nav.replace(destination)
    }

    private fun saveContinueTarget(state: PlayerState.State) {
        val request = playbackProgressHandler.continueTargetRequest(state) ?: return
        viewModelScope.launch {
            playbackProgressHandler.saveContinueTarget(request)
        }
    }

    private fun saveWatchedProgressIfNeeded(positionMs: Long, durationMs: Long) {
        val completionState = currentState
        val request = playbackProgressHandler.watchedProgressRequest(
            state = completionState,
            positionMs = positionMs,
            durationMs = durationMs,
        ) ?: return
        val nextState = sourceSelectionHandler.nextEpisode(completionState)
        viewModelScope.launch {
            playbackProgressHandler.saveProgress(request)
            val nextTargetRequest =
                if (playbackProgressHandler.shouldSuggestNextEpisodeOnWatched()) {
                    nextState?.let(playbackProgressHandler::continueTargetRequest)
                } else {
                    null
                }
            if (nextTargetRequest != null) {
                playbackProgressHandler.saveContinueTarget(nextTargetRequest)
            } else {
                playbackProgressHandler.suppressContinueWatchingDisplay(completionState)
            }
        }
    }

    /**
     * Применяет выбранный пользователем источник и запускает загрузку потока.
     *
     * Переключение серии сначала обновляет `/videos`, а смена балансера или озвучки может
     * использовать уже загруженный граф источников.
     */
    private fun applySourceSelection(
        state: PlayerState.State?,
        sourceScopeChanged: Boolean = false,
        resumeMode: PlayerStreamResumeMode = PlayerStreamResumeMode.PreserveCurrent,
        refreshSourcesBeforeStream: Boolean = false,
    ) {
        if (state == null) return
        allohaRecovery.reset()
        allohaPlaybackRecoveryJob?.cancel()
        playbackRetry.reset()
        playbackRetryJob?.cancel()
        setState { sourceStreamHandler.preparingStreamLoad(state, resumeMode) }
        if (sourceScopeChanged) {
            observeActivePlayerResizeSettings()
            observeActivePlayerMobileVideoTransformSettings()
        }
        if (refreshSourcesBeforeStream) {
            refreshSourceGraphThenLoadStream(resumeMode)
        } else {
            loadStream(resumeMode)
        }
    }

    private fun isActivePlaybackSource(episodeUrl: String): Boolean =
        episodeUrl.isBlank() || episodeUrl == activeIframeUrl(currentState)

    /** Обновляет граф источников из сети и один раз запускает получение потока. */
    private fun refreshSourceGraphThenLoadStream(
        resumeMode: PlayerStreamResumeMode = PlayerStreamResumeMode.PreserveCurrent,
    ) {
        loadSourceGraph(
            forceRefreshVideos = true,
            loadStreamOnFailure = true,
            loadStreamAfterRefresh = true,
            resumeMode = resumeMode,
            refreshStreamOnFailure = false,
        )
    }

    /**
     * Запускает загрузку графа источников и применяет результат handler-а к состоянию экрана.
     *
     * Проверка активного destination остается здесь, чтобы устаревший результат от старого экрана
     * не обновил текущий плеер.
     */
    private fun loadSourceGraph(
        forceRefreshVideos: Boolean = false,
        loadStreamOnFailure: Boolean = false,
        loadStreamAfterRefresh: Boolean = false,
        resumeMode: PlayerStreamResumeMode = PlayerStreamResumeMode.PreserveCurrent,
        refreshStreamOnFailure: Boolean = !forceRefreshVideos,
    ) {
        val destination = activeDest
        sourceGraphJob?.cancel()
        sourceGraphJob = viewModelScope.launch {
            when (val result = sourceStreamHandler.loadSourceGraph(
                state = currentState,
                forceRefreshVideos = forceRefreshVideos,
                loadStreamOnFailure = loadStreamOnFailure,
                loadStreamAfterRefresh = loadStreamAfterRefresh,
                resumeMode = resumeMode,
                refreshStreamOnFailure = refreshStreamOnFailure,
            )) {
                PlayerSourceGraphLoadResult.Ignore -> Unit

                is PlayerSourceGraphLoadResult.LoadStream -> {
                    loadStream(
                        resumeMode = result.resumeMode,
                        refreshSourcesOnFailure = result.refreshSourcesOnFailure,
                    )
                }

                is PlayerSourceGraphLoadResult.SourceGraph -> {
                    if (destination != activeDest) return@launch

                    val previousIframeUrl = activeIframeUrl(currentState)
                    setState {
                        sourceStreamHandler.applySourceGraph(
                            state = this,
                            sourceGraph = result.sourceGraph,
                        )
                    }
                    observeActivePlayerResizeSettings()
                    observeActivePlayerMobileVideoTransformSettings()
                    if (
                        result.loadStreamAfterRefresh ||
                        activeIframeUrl(currentState) != previousIframeUrl
                    ) {
                        loadStream(
                            resumeMode = result.resumeMode,
                            refreshSourcesOnFailure = result.refreshStreamOnFailure,
                        )
                    }
                }
            }
        }
    }

    /** Подписывается на настройки размера для текущей пары тайтл/плеер. */
    private fun observeActivePlayerResizeSettings(force: Boolean = false) {
        val scope = currentPlayerResizeSettingsScope()
        val changed = displaySettingsHandler.observeResizeSettings(
            scope = scope,
            coroutineScope = viewModelScope,
            force = force,
            onChanged = { settings ->
                setState {
                    copy(
                        resizeMode = settings.resizeMode,
                        zoomLevel = settings.zoomLevel,
                    )
                }
            },
        )
        if (changed) {
            setState {
                copy(
                    resizeMode = PlayerResizeMode.FIT,
                    zoomLevel = PlayerZoomLevel.PERCENT_10,
                )
            }
        }
    }

    /** Сохраняет настройки размера для текущей пары тайтл/плеер. */
    private fun savePlayerResizeSettings(settings: PlayerResizeSettings) {
        val scope = currentPlayerResizeSettingsScope()
        displaySettingsHandler.saveResizeSettings(scope, settings, viewModelScope)
    }

    /** Подписывается на мобильные настройки масштаба и смещения для текущей пары тайтл/плеер. */
    private fun observeActivePlayerMobileVideoTransformSettings(force: Boolean = false) {
        val scope = currentPlayerResizeSettingsScope()
        val changed = displaySettingsHandler.observeMobileTransformSettings(
            scope = scope,
            coroutineScope = viewModelScope,
            force = force,
            onChanged = { settings ->
                setState {
                    copy(
                        mobileVideoScale = settings.scale,
                        mobileVideoOffsetX = settings.offsetX,
                        mobileVideoOffsetY = settings.offsetY,
                    )
                }
            },
        )
        if (changed) {
            setState {
                copy(
                    mobileVideoScale = 1f,
                    mobileVideoOffsetX = 0f,
                    mobileVideoOffsetY = 0f,
                )
            }
        }
    }

    /** Сохраняет мобильные настройки масштаба и смещения для текущей пары тайтл/плеер. */
    private fun savePlayerMobileVideoTransformSettings(settings: PlayerMobileVideoTransformSettings) {
        val scope = currentPlayerResizeSettingsScope()
        displaySettingsHandler.saveMobileTransformSettings(scope, settings, viewModelScope)
    }

    /** Возвращает ключ хранения, общий для TV-настроек размера и мобильного transform. */
    private fun currentPlayerResizeSettingsScope(): PlayerResizeSettingsScope {
        return sourceSelectionHandler.resizeSettingsScope(currentState)
    }

    /**
     * Запускает получение потока для активного источника.
     *
     * Первая ошибка resolve может запросить принудительное обновление источников; повтор отключает
     * этот флаг, чтобы последующие ошибки показались пользователю без бесконечного цикла.
     */
    private fun loadStream(
        resumeMode: PlayerStreamResumeMode = PlayerStreamResumeMode.PreserveCurrent,
        refreshSourcesOnFailure: Boolean = true,
        forceFreshAllohaSession: Boolean = false,
        selectedQualityOverride: String? = null,
        forceRefresh: Boolean = false,
    ) {
        if (resumeMode == PlayerStreamResumeMode.SelectedSourceOnly) {
            pendingDestinationResumeMs = null
        }
        extractionJob?.cancel()
        extractionJob = viewModelScope.launch {
            val preserveStreamDuringAllohaRecovery =
                allohaRecovery.isRecovering && currentState.streamUrl != null
            val wasAlreadyResolving = currentState.streamUrl == null &&
                    currentState.playerError == null &&
                    currentState.kodikBlockedError == null
            val hintTimerAlreadyRunning =
                wasAlreadyResolving && streamLoadingHintJob?.isActive == true
            if (!hintTimerAlreadyRunning && !preserveStreamDuringAllohaRecovery) {
                startChangePlayerHintTimer()
            }
            val canPreserveCurrent = resumeMode == PlayerStreamResumeMode.PreserveCurrent
            val stateResumeMs = allohaRecovery.positionMs
                .takeIf { canPreserveCurrent && allohaRecovery.isRecovering && it > 0L }
                ?: currentState.playbackPositionMs.takeIf { canPreserveCurrent && it > 0L }
                ?: currentState.resumeFromMs.takeIf { canPreserveCurrent && it > 0L }
            val destinationResumeMs = pendingDestinationResumeMs.takeIf { canPreserveCurrent }
            setState {
                sourceStreamHandler.preparingStreamResolve(
                    state = this,
                    preserveCurrentStream = preserveStreamDuringAllohaRecovery,
                )
            }
            val s = currentState
            val pendingResume = s.dubbingResumeMs.takeIf { canPreserveCurrent && it >= 0L }
                ?: destinationResumeMs
                ?: stateResumeMs
            when (val result = sourceStreamHandler.resolveStream(
                state = s,
                pendingResume = pendingResume,
                destinationResumeMs = destinationResumeMs,
                resumeMode = resumeMode,
                refreshSourcesOnFailure = refreshSourcesOnFailure,
                reuseAllohaPlaybackSession = !forceFreshAllohaSession,
                selectedQualityOverride = selectedQualityOverride,
                forceRefresh = forceRefresh,
            )) {
                is PlayerStreamLoadResult.RefreshSources -> {
                    refreshSourceGraphThenLoadStream(result.resumeMode)
                }

                is PlayerStreamLoadResult.State -> {
                    val resolveFailed = result.state.playerError != null ||
                            result.state.kodikBlockedError != null
                    val completedAllohaPlaybackRecovery =
                        allohaRecovery.isRecovering &&
                                !currentState.isOfflinePlayback &&
                                currentState.isAllohaSource()
                    if (!resolveFailed) {
                        allohaSession.activate(result.allohaSession, viewModelScope)
                        result.state.selectedQuality?.let { quality ->
                            allohaSession.selectQuality(quality)
                        }
                    }
                    if (
                        allohaRecovery.isRecovering &&
                        !currentState.isOfflinePlayback &&
                        currentState.isAllohaSource() &&
                        resolveFailed
                    ) {
                        // No retry cap here, matching the reference implementation: a warm, pooled
                        // WebView (see AllohaExtractor) makes each fresh-session attempt cheap
                        // enough that retrying indefinitely is fine for transient CDN/token
                        // rejections. A source that's permanently unavailable (not just
                        // temporarily rejected) will keep retrying too - there's currently no way
                        // to distinguish the two failure kinds here - so the user still needs to
                        // navigate away or switch dubbing/balancer manually in that case.
                        scheduleFreshAllohaPlaybackAttempt(ALLOHA_PLAYBACK_RECOVERY_DELAY_MS)
                        return@launch
                    }
                    val completedRecoveryAttempts = allohaRecovery.complete()
                    allohaPlaybackRecoveryJob?.cancel()
                    if (result.consumedDestinationResume) {
                        pendingDestinationResumeMs = null
                    }
                    streamLoadingHintJob?.cancel()
                    if (completedAllohaPlaybackRecovery) {
                        if (resolveFailed) {
                            Log.w(
                                LOG_TAG,
                                "Background Alloha playback recovery failed " +
                                        "attempts=$completedRecoveryAttempts",
                            )
                        } else {
                            Log.i(
                                LOG_TAG,
                                "Background Alloha playback recovery stream resolved " +
                                        "attempts=$completedRecoveryAttempts " +
                                        "positionMs=${result.state.resumeFromMs.coerceAtLeast(0L)}",
                            )
                        }
                    }
                    setState {
                        copy(
                            streamUrl = result.state.streamUrl,
                            streamHeaders = result.state.streamHeaders,
                            streamQualityMap = result.state.streamQualityMap,
                            selectedQuality = result.state.selectedQuality,
                            allohaAudioTracks = result.state.allohaAudioTracks,
                            selectedAllohaAudioId = result.state.selectedAllohaAudioId,
                            allohaSubtitles = result.state.allohaSubtitles,
                            selectedAllohaSubtitleIndex = result.state.selectedAllohaSubtitleIndex,
                            playerError = result.state.playerError,
                            kodikBlockedError = result.state.kodikBlockedError,
                            resumeFromMs = result.state.resumeFromMs,
                            dubbingResumeMs = result.state.dubbingResumeMs,
                            retryKey = if (completedAllohaPlaybackRecovery && !resolveFailed) {
                                retryKey + 1
                            } else {
                                retryKey
                            },
                            isPlaybackRecovering =
                                completedAllohaPlaybackRecovery && !resolveFailed,
                            showChangePlayerHint = false,
                        )
                    }
                    if (!resolveFailed) syncBackStackDestination()
                    if (!resolveFailed && currentState.isAllohaSource()) {
                        restoreAllohaTrackPreference(
                            audioTracks = result.state.allohaAudioTracks,
                            subtitles = result.state.allohaSubtitles,
                        )
                    }
                }
            }
        }
    }

    /**
     * Применяет ранее сохранённый выбор аудиодорожки/субтитров Alloha (по [PlayerAllohaTrackPreferenceHandler])
     * к только что распарсенным спискам, если он отличается от дефолта экстрактора.
     */
    private suspend fun restoreAllohaTrackPreference(
        audioTracks: List<AllohaAudioTrack>,
        subtitles: List<AllohaSubtitleTrack>,
    ) {
        if (audioTracks.isEmpty() && subtitles.isEmpty()) return
        val match = allohaTrackPreference.findMatch(
            animeId = currentState.animeId,
            dubbing = activeDubbingName(currentState),
            player = activeBalancerName(currentState),
            audioTracks = audioTracks,
            subtitles = subtitles,
        ) ?: return

        val audioId = match.audioId
        if (audioId != null && audioId != currentState.selectedAllohaAudioId) {
            val stream = allohaSession.selectAudioTrack(audioId)
            if (stream != null) {
                setState {
                    copy(
                        selectedAllohaAudioId = stream.selectedAllohaAudioId,
                        streamQualityMap = stream.qualities,
                        selectedQuality = selectedQuality?.takeIf {
                            stream.qualities?.containsKey(it) == true
                        },
                        streamUrl = stream.url,
                    )
                }
            }
        }

        if (match.applySubtitleChange && match.subtitleIndex != currentState.selectedAllohaSubtitleIndex) {
            val index = match.subtitleIndex
            setState {
                copy(selectedAllohaSubtitleIndex = index?.takeIf { it in subtitles.indices })
            }
        }
    }

    /** Запоминает выбранную пользователем аудиодорожку Alloha для текущей озвучки тайтла. */
    private fun saveAllohaAudioPreference(audioId: String) {
        val label = currentState.allohaAudioTracks.firstOrNull { it.id == audioId }?.label ?: return
        val animeId = currentState.animeId
        val dubbing = activeDubbingName(currentState)
        val player = activeBalancerName(currentState)
        viewModelScope.launch {
            allohaTrackPreference.saveAudioSelection(
                animeId = animeId,
                dubbing = dubbing,
                player = player,
                audioLabel = label,
            )
        }
    }

    /** Запоминает выбор субтитров Alloha (или их отключение) для текущей озвучки тайтла. */
    private fun saveAllohaSubtitlePreference(index: Int?) {
        val subtitle = index?.let { currentState.allohaSubtitles.getOrNull(it) }
        val animeId = currentState.animeId
        val dubbing = activeDubbingName(currentState)
        val player = activeBalancerName(currentState)
        viewModelScope.launch {
            allohaTrackPreference.saveSubtitleSelection(
                animeId = animeId,
                dubbing = dubbing,
                player = player,
                subtitleLanguage = subtitle?.language,
                subtitleLabel = subtitle?.label,
                subtitleOff = index == null,
            )
        }
    }

    /** Показывает подсказку "сменить плеер" в UI, если поток не резолвится дольше [CHANGE_PLAYER_HINT_DELAY_MS]. */
    private fun startChangePlayerHintTimer() {
        streamLoadingHintJob?.cancel()
        setState { copy(showChangePlayerHint = false) }
        val destination = activeDest
        streamLoadingHintJob = viewModelScope.launch {
            delay(CHANGE_PLAYER_HINT_DELAY_MS)
            if (destination == activeDest) {
                setState { copy(showChangePlayerHint = true) }
            }
        }
    }

    private fun startAllohaPlaybackRecovery(
        positionMs: Long,
        selectedQuality: String?,
        initialDelayMs: Long,
    ) {
        extractionJob?.cancel()
        allohaPlaybackRecoveryJob?.cancel()
        allohaSession.close()
        val resumePosition = positionMs.coerceAtLeast(0L)
        allohaRecovery.start(resumePosition, selectedQuality)
        streamLoadingHintJob?.cancel()
        setState {
            copy(
                playerError = null,
                kodikBlockedError = null,
                resumeFromMs = resumePosition,
                playbackPositionMs = resumePosition,
                isPlaybackRecovering = true,
                showChangePlayerHint = false,
            )
        }
        // Если восстановление затянулось дольше [ALLOHA_RECOVERY_HINT_DELAY_MS] - предлагаем юзеру
        // сменить плеер/озвучку, не дожидаясь исчерпания попыток.
        streamLoadingHintJob = viewModelScope.launch {
            delay(ALLOHA_RECOVERY_HINT_DELAY_MS)
            if (allohaRecovery.isRecovering) {
                setState { copy(showChangePlayerHint = true) }
            }
        }
        Log.i(
            LOG_TAG,
            "Starting fresh Alloha playback recovery positionMs=$resumePosition " +
                    "quality=${selectedQuality ?: "auto"}",
        )
        scheduleFreshAllohaPlaybackAttempt(initialDelayMs)
    }

    private fun scheduleFreshAllohaPlaybackAttempt(delayMs: Long) {
        allohaPlaybackRecoveryJob?.cancel()
        if (!allohaRecovery.canRetry()) {
            // Попытки исчерпаны: снимаем оверлей восстановления и показываем настоящую ошибку с
            // действиями, вместо того чтобы крутиться дальше без шанса на успех.
            Log.w(
                LOG_TAG,
                "Alloha playback recovery giving up after " +
                        "${PlayerAllohaRecoveryHandler.MAX_ATTEMPTS} attempts",
            )
            allohaRecovery.reset()
            streamLoadingHintJob?.cancel()
            setState {
                copy(
                    isPlaybackRecovering = false,
                    showChangePlayerHint = true,
                    playerError = sourceStreamHandler.playbackErrorMessage(
                        strings.get(R.string.player_stream_error),
                    ),
                )
            }
            return
        }
        val destination = activeDest
        val iframeUrl = activeIframeUrl(currentState)
        val attempt = allohaRecovery.nextAttempt()
        allohaPlaybackRecoveryJob = viewModelScope.launch {
            delay(delayMs)
            if (
                destination == activeDest &&
                activeIframeUrl(currentState) == iframeUrl &&
                !currentState.isOfflinePlayback &&
                currentState.isAllohaSource() &&
                allohaRecovery.isRecovering
            ) {
                allohaSession.close()
                Log.i(
                    LOG_TAG,
                    "Opening fresh Alloha playback session " +
                            "attempt=$attempt/${PlayerAllohaRecoveryHandler.MAX_ATTEMPTS} " +
                            "positionMs=${allohaRecovery.positionMs}",
                )
                loadStream(
                    refreshSourcesOnFailure = false,
                    forceFreshAllohaSession = true,
                    selectedQualityOverride = allohaRecovery.selectedQuality,
                )
            }
        }
    }

    /**
     * Тихий повтор воспроизведения для не-Alloha плееров: держим последний кадр + спиннер и молча
     * перерезолвим источник. До [PlayerPlaybackRetryHandler.MAX_ATTEMPTS] раз, дальше - оверлей ошибки.
     */
    private fun schedulePlaybackRetryAttempt() {
        playbackRetryJob?.cancel()
        val destination = activeDest
        val iframeUrl = activeIframeUrl(currentState)
        val attempt = playbackRetry.next()
        streamLoadingHintJob?.cancel()
        setState {
            copy(
                playerError = null,
                isPlaybackRecovering = true,
                showChangePlayerHint = false,
            )
        }
        Log.i(
            LOG_TAG,
            "Silent playback retry attempt=$attempt/${PlayerPlaybackRetryHandler.MAX_ATTEMPTS}",
        )
        playbackRetryJob = viewModelScope.launch {
            // Re-resolve starts immediately (no artificial delay) so the visible stall is bounded
            // by the resolve+rebuild time only, not padded by a fixed wait before it even begins.
            if (
                destination == activeDest &&
                activeIframeUrl(currentState) == iframeUrl &&
                !currentState.isOfflinePlayback
            ) {
                setState { copy(retryKey = retryKey + 1) }
                allohaSession.close()
                loadStream(
                    refreshSourcesOnFailure = true,
                    selectedQualityOverride = currentState.selectedQuality,
                    forceRefresh = true,
                )
            }
        }
    }

    override fun onCleared() {
        allohaPlaybackRecoveryJob?.cancel()
        playbackRetryJob?.cancel()
        allohaSession.close(immediately = false)
        streamLoadingHintJob?.cancel()
        super.onCleared()
    }

    private companion object {
        fun PlayerState.State.isAllohaSource(): Boolean =
            activeBalancerName(this).contains(ALLOHA_PLAYER_NAME, ignoreCase = true)

        private const val ALLOHA_PLAYER_NAME = "alloha"
        private const val LOG_TAG = "PlayerViewModel"
        private const val ALLOHA_PLAYBACK_RECOVERY_DELAY_MS = 1_000L
        private const val CHANGE_PLAYER_HINT_DELAY_MS = 10_000L
        private const val ALLOHA_RECOVERY_HINT_DELAY_MS = 15_000L
    }

}
