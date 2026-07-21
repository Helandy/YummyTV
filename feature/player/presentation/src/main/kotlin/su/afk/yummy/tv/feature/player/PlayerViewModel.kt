package su.afk.yummy.tv.feature.player

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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
import su.afk.yummy.tv.core.preferences.settings.PlayerMobileVideoTransformSettings
import su.afk.yummy.tv.core.preferences.settings.PlayerResizeMode
import su.afk.yummy.tv.core.preferences.settings.PlayerResizeSettings
import su.afk.yummy.tv.core.preferences.settings.PlayerZoomLevel
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.player.session.AllohaPlaybackSessionManager
import su.afk.yummy.tv.domain.videodownload.usecase.GetVideoDownloadUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.player.PlayerViewModel.Companion.CHANGE_PLAYER_HINT_DELAY_MS
import su.afk.yummy.tv.feature.player.handler.PlayerPlaybackProgressHandler
import su.afk.yummy.tv.feature.player.handler.PlayerSettingsHandler
import su.afk.yummy.tv.feature.player.handler.PlayerSourceGraphLoadResult
import su.afk.yummy.tv.feature.player.handler.PlayerSourceSelectionHandler
import su.afk.yummy.tv.feature.player.handler.PlayerSourceStreamHandler
import su.afk.yummy.tv.feature.player.handler.PlayerStreamLoadResult
import su.afk.yummy.tv.feature.player.handler.PlayerStreamResumeMode
import su.afk.yummy.tv.feature.player.navigator.PlayerDestination
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.utils.PlayerResizeSettingsScope
import su.afk.yummy.tv.feature.player.utils.activeBalancerName
import su.afk.yummy.tv.feature.player.utils.activeIframeUrl

@HiltViewModel(assistedFactory = PlayerViewModel.Factory::class)
class PlayerViewModel @AssistedInject internal constructor(
    @Assisted private val dest: PlayerDestination,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val sourceStreamHandler: PlayerSourceStreamHandler,
    private val playbackProgressHandler: PlayerPlaybackProgressHandler,
    private val settingsHandler: PlayerSettingsHandler,
    private val destinationStateMapper: PlayerDestinationStateMapper,
    private val sourceSelectionHandler: PlayerSourceSelectionHandler,
    private val getVideoDownload: GetVideoDownloadUseCase,
    private val strings: StringProvider,
    private val analytics: PlayerAnalytics,
    private val allohaSessionManager: AllohaPlaybackSessionManager,
) : BaseViewModelNew<PlayerState.State, PlayerState.Event, PlayerState.Effect>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(dest: PlayerDestination): PlayerViewModel
    }

    private var activeDest: PlayerDestination = dest
    private var pendingDestinationResumeMs: Long? = dest.resumeFromMs.takeIf { it > 0L }
    private var sourceGraphJob: Job? = null
    private var allohaPlaybackRecoveryJob: Job? = null
    private var isRecoveringAllohaPlayback = false
    private var allohaPlaybackRecoveryRetryCount = 0
    private var allohaPlaybackRecoveryQuality: String? = null
    private var allohaPlaybackRecoveryPositionMs = 0L
    private var activeAllohaSession: AllohaStreamSession? = null
    private var allohaSessionRefreshJob: Job? = null
    private var streamLoadingHintJob: Job? = null
    private var mobileGestureTutorialReady = false
    private var showMobileGestureTutorial = false
    private var isNavigatingToRating = false

    fun loadDestination(newDest: PlayerDestination) {
        if (newDest == activeDest) return
        closeAllohaSession()
        isRecoveringAllohaPlayback = false
        allohaPlaybackRecoveryRetryCount = 0
        allohaPlaybackRecoveryQuality = null
        allohaPlaybackRecoveryPositionMs = 0L
        allohaPlaybackRecoveryJob?.cancel()
        activeDest = newDest
        pendingDestinationResumeMs = newDest.resumeFromMs.takeIf { it > 0L }
        setState {
            destinationStateMapper.toState(
                newDest,
                autoSkipOpeningsEndings = autoSkipOpeningsEndings,
                autoPlayNextEpisode = autoPlayNextEpisode,
                pictureInPictureEnabled = pictureInPictureEnabled,
            ).copy(
                mobileGestureTutorialReady = mobileGestureTutorialReady,
                showMobileGestureTutorial = showMobileGestureTutorial,
            )
        }
        observeActivePlayerResizeSettings(force = true)
        observeActivePlayerMobileVideoTransformSettings(force = true)
        if (newDest.downloadId > 0L) {
            loadDownloadedDestination(newDest.downloadId)
        } else {
            loadSourceGraph()
            loadStream()
        }
    }

    override fun createInitialState() = destinationStateMapper.toState(dest)

    private var extractionJob: Job? = null
    private var playerResizeSettingsJob: Job? = null
    private var activePlayerResizeSettingsScope: PlayerResizeSettingsScope? = null
    private var playerMobileVideoTransformSettingsJob: Job? = null
    private var playerMobileVideoTransformSaveJob: Job? = null
    private var activePlayerMobileVideoTransformSettingsScope: PlayerResizeSettingsScope? = null

    init {
        analytics.eventScreenOpened(dest.animeId)
        settingsHandler.autoSkipOpeningsEndings
            .onEach { enabled -> setState { copy(autoSkipOpeningsEndings = enabled) } }
            .launchIn(viewModelScope)
        settingsHandler.autoPlayNextEpisode
            .onEach { enabled -> setState { copy(autoPlayNextEpisode = enabled) } }
            .launchIn(viewModelScope)
        settingsHandler.pictureInPictureEnabled
            .onEach { enabled -> setState { copy(pictureInPictureEnabled = enabled) } }
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
        if (dest.downloadId > 0L) {
            loadDownloadedDestination(dest.downloadId)
        } else {
            observeActivePlayerResizeSettings()
            observeActivePlayerMobileVideoTransformSettings()
            loadSourceGraph()
            loadStream()
        }
    }

    override fun onEvent(event: PlayerState.Event) {
        when (event) {
            PlayerState.Event.Back -> saveCurrentProgressThenNavigate {
                closeAllohaSession()
                nav.back()
            }

            PlayerState.Event.MobileGestureTutorialDismissed -> {
                showMobileGestureTutorial = false
                setState { copy(showMobileGestureTutorial = false) }
                viewModelScope.launch {
                    settingsHandler.dismissMobilePlayerGestureTutorial()
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
                if (currentState.isOfflinePlayback) {
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
                    closeAllohaSession()
                    loadStream(refreshSourcesOnFailure = true)
                }
            }

            PlayerState.Event.TvAppBackgrounded -> {
                if (!isNavigatingToRating) returnToDetailsAfterTvBackground()
            }

            PlayerState.Event.RateTitle -> {
                analytics.eventRateTitle(currentState.animeId)
                val animeId = currentState.animeId
                if (animeId > 0 && !isNavigatingToRating) {
                    isNavigatingToRating = true
                    saveCurrentProgressThenNavigate {
                        navigateFromPlayerToRating(animeId)
                    }
                }
            }

            is PlayerState.Event.PlaybackError -> {
                analytics.eventPlaybackError(
                    state = currentState,
                    message = event.message,
                    errorCode = event.errorCode,
                    errorType = event.errorType,
                )
                if (!currentState.isOfflinePlayback && currentState.isAllohaSource()) {
                    if (isRecoveringAllohaPlayback) {
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
                } else {
                    streamLoadingHintJob?.cancel()
                    setState {
                        copy(
                            streamUrl = null,
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
                if (
                    currentState.isAllohaPlaybackRecovering &&
                    !isRecoveringAllohaPlayback &&
                    !currentState.isOfflinePlayback &&
                    currentState.isAllohaSource()
                ) {
                    Log.i(
                        LOG_TAG,
                        "Background Alloha playback recovery ready " +
                                "positionMs=${currentState.playbackPositionMs.coerceAtLeast(0L)}",
                    )
                    setState { copy(isAllohaPlaybackRecovering = false) }
                }
            }

            PlayerState.Event.PrevEpisode -> {
                closeAllohaSession()
                analytics.eventPrevEpisode(currentState.animeId)
                applySourceSelection(
                    sourceSelectionHandler.previousEpisode(currentState),
                    resumeMode = PlayerStreamResumeMode.SelectedSourceOnly,
                    refreshSourcesBeforeStream = true,
                )
            }

            is PlayerState.Event.NextEpisode -> {
                closeAllohaSession()
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
                closeAllohaSession()
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
                closeAllohaSession()
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
                if (isRecoveringAllohaPlayback) {
                    allohaPlaybackRecoveryQuality = event.quality
                }
                activeAllohaSession?.selectQuality(event.quality)
                setState {
                    copy(
                        selectedQuality = event.quality,
                        resumeFromMs = position,
                        playbackPositionMs = position,
                    )
                }
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
                if (isRecoveringAllohaPlayback && position > 0L) {
                    allohaPlaybackRecoveryPositionMs = position
                }
                setState {
                    copy(
                        resumeFromMs = position,
                        playbackPositionMs = position,
                        playbackDurationMs = duration,
                    )
                }
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
        closeAllohaSession()
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
                    isOfflinePlayback = true,
                    offlineCacheKey = item.cacheKey,
                    playerError = null,
                )
            }
        }
    }

    private fun PlayerState.State.isAllohaSource(): Boolean =
        activeBalancerName(this).contains(ALLOHA_PLAYER_NAME, ignoreCase = true)

    private fun saveCurrentProgressThenNavigate(
        syncRemote: Boolean = true,
        navigate: () -> Unit,
    ) {
        val request = playbackProgressHandler.currentProgressSaveRequest(
            state = currentState,
            syncRemote = syncRemote,
        )
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

    private fun returnToDetailsAfterTvBackground() {
        val animeId = currentState.animeId
        saveCurrentProgressThenNavigate(syncRemote = false) {
            closeAllohaSession()
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
        }
    }

    private fun navigateFromPlayerToRating(animeId: Int) {
        val detailsDestination = detailsNavigator.getDetailsDest(animeId)
        val ratingDestination = detailsNavigator.getRatingDest(animeId)
        val previousDestination = nav.backStack.getOrNull(nav.backStack.lastIndex - 1)
        if (previousDestination == detailsDestination) {
            nav.replace(ratingDestination)
        } else {
            nav.replace(detailsDestination)
            nav.navigate(ratingDestination)
        }
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
        isRecoveringAllohaPlayback = false
        allohaPlaybackRecoveryRetryCount = 0
        allohaPlaybackRecoveryQuality = null
        allohaPlaybackRecoveryPositionMs = 0L
        allohaPlaybackRecoveryJob?.cancel()
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
        if (!force && scope == activePlayerResizeSettingsScope) return
        activePlayerResizeSettingsScope = scope
        playerResizeSettingsJob?.cancel()
        setState {
            copy(
                resizeMode = PlayerResizeMode.FIT,
                zoomLevel = PlayerZoomLevel.PERCENT_10,
            )
        }
        playerResizeSettingsJob = settingsHandler
            .observeResizeSettings(scope)
            .onEach { settings ->
                if (scope == activePlayerResizeSettingsScope) {
                    setState {
                        copy(
                            resizeMode = settings.resizeMode,
                            zoomLevel = settings.zoomLevel
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /** Сохраняет настройки размера для текущей пары тайтл/плеер. */
    private fun savePlayerResizeSettings(settings: PlayerResizeSettings) {
        val scope = currentPlayerResizeSettingsScope()
        viewModelScope.launch {
            settingsHandler.saveResizeSettings(scope, settings)
        }
    }

    /** Подписывается на мобильные настройки масштаба и смещения для текущей пары тайтл/плеер. */
    private fun observeActivePlayerMobileVideoTransformSettings(force: Boolean = false) {
        val scope = currentPlayerResizeSettingsScope()
        if (!force && scope == activePlayerMobileVideoTransformSettingsScope) return
        activePlayerMobileVideoTransformSettingsScope = scope
        playerMobileVideoTransformSettingsJob?.cancel()
        playerMobileVideoTransformSaveJob?.cancel()
        setState {
            copy(
                mobileVideoScale = 1f,
                mobileVideoOffsetX = 0f,
                mobileVideoOffsetY = 0f,
            )
        }
        playerMobileVideoTransformSettingsJob = settingsHandler
            .observeMobileVideoTransformSettings(scope)
            .onEach { settings ->
                if (scope == activePlayerMobileVideoTransformSettingsScope) {
                    setState {
                        copy(
                            mobileVideoScale = settings.scale,
                            mobileVideoOffsetX = settings.offsetX,
                            mobileVideoOffsetY = settings.offsetY,
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /** Сохраняет мобильные настройки масштаба и смещения для текущей пары тайтл/плеер. */
    private fun savePlayerMobileVideoTransformSettings(settings: PlayerMobileVideoTransformSettings) {
        val scope = currentPlayerResizeSettingsScope()
        playerMobileVideoTransformSaveJob?.cancel()
        playerMobileVideoTransformSaveJob = viewModelScope.launch {
            settingsHandler.saveMobileVideoTransformSettings(scope, settings)
        }
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
    ) {
        if (resumeMode == PlayerStreamResumeMode.SelectedSourceOnly) {
            pendingDestinationResumeMs = null
        }
        extractionJob?.cancel()
        extractionJob = viewModelScope.launch {
            val preserveStreamDuringAllohaRecovery =
                isRecoveringAllohaPlayback && currentState.streamUrl != null
            val wasAlreadyResolving = currentState.streamUrl == null &&
                    currentState.playerError == null &&
                    currentState.kodikBlockedError == null
            val hintTimerAlreadyRunning =
                wasAlreadyResolving && streamLoadingHintJob?.isActive == true
            if (!hintTimerAlreadyRunning && !preserveStreamDuringAllohaRecovery) {
                startChangePlayerHintTimer()
            }
            val canPreserveCurrent = resumeMode == PlayerStreamResumeMode.PreserveCurrent
            val stateResumeMs = allohaPlaybackRecoveryPositionMs
                .takeIf { canPreserveCurrent && isRecoveringAllohaPlayback && it > 0L }
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
            )) {
                is PlayerStreamLoadResult.RefreshSources -> {
                    refreshSourceGraphThenLoadStream(result.resumeMode)
                }

                is PlayerStreamLoadResult.State -> {
                    val resolveFailed = result.state.playerError != null ||
                            result.state.kodikBlockedError != null
                    val completedAllohaPlaybackRecovery =
                        isRecoveringAllohaPlayback &&
                                !currentState.isOfflinePlayback &&
                                currentState.isAllohaSource()
                    if (!resolveFailed) {
                        activateAllohaSession(result.allohaSession)
                        result.state.selectedQuality?.let { quality ->
                            activeAllohaSession?.selectQuality(quality)
                        }
                    }
                    if (
                        isRecoveringAllohaPlayback &&
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
                    val completedRecoveryAttempts = allohaPlaybackRecoveryRetryCount
                    isRecoveringAllohaPlayback = false
                    allohaPlaybackRecoveryRetryCount = 0
                    allohaPlaybackRecoveryQuality = null
                    allohaPlaybackRecoveryPositionMs = 0L
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
                            playerError = result.state.playerError,
                            kodikBlockedError = result.state.kodikBlockedError,
                            resumeFromMs = result.state.resumeFromMs,
                            dubbingResumeMs = result.state.dubbingResumeMs,
                            retryKey = if (completedAllohaPlaybackRecovery && !resolveFailed) {
                                retryKey + 1
                            } else {
                                retryKey
                            },
                            isAllohaPlaybackRecovering =
                                completedAllohaPlaybackRecovery && !resolveFailed,
                            showChangePlayerHint = false,
                        )
                    }
                }
            }
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
        closeAllohaSession()
        isRecoveringAllohaPlayback = true
        allohaPlaybackRecoveryRetryCount = 0
        allohaPlaybackRecoveryQuality = selectedQuality
        val resumePosition = positionMs.coerceAtLeast(0L)
        allohaPlaybackRecoveryPositionMs = resumePosition
        streamLoadingHintJob?.cancel()
        setState {
            copy(
                playerError = null,
                kodikBlockedError = null,
                resumeFromMs = resumePosition,
                playbackPositionMs = resumePosition,
                isAllohaPlaybackRecovering = true,
                showChangePlayerHint = false,
            )
        }
        // Восстановление ретраится без лимита, поэтому если оно затянулось дольше
        // [ALLOHA_RECOVERY_HINT_DELAY_MS] - предлагаем юзеру сменить плеер/озвучку.
        streamLoadingHintJob = viewModelScope.launch {
            delay(ALLOHA_RECOVERY_HINT_DELAY_MS)
            if (isRecoveringAllohaPlayback) {
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
        val destination = activeDest
        val iframeUrl = activeIframeUrl(currentState)
        val attempt = ++allohaPlaybackRecoveryRetryCount
        allohaPlaybackRecoveryJob = viewModelScope.launch {
            delay(delayMs)
            if (
                destination == activeDest &&
                activeIframeUrl(currentState) == iframeUrl &&
                !currentState.isOfflinePlayback &&
                currentState.isAllohaSource() &&
                isRecoveringAllohaPlayback
            ) {
                closeAllohaSession()
                Log.i(
                    LOG_TAG,
                    "Opening fresh Alloha playback session attempt=$attempt " +
                            "positionMs=$allohaPlaybackRecoveryPositionMs",
                )
                loadStream(
                    refreshSourcesOnFailure = false,
                    forceFreshAllohaSession = true,
                    selectedQualityOverride = allohaPlaybackRecoveryQuality,
                )
            }
        }
    }

    private fun activateAllohaSession(session: AllohaStreamSession?) {
        if (activeAllohaSession === session) return
        closeAllohaSession()
        activeAllohaSession = session?.let(allohaSessionManager::activate) ?: return
        allohaSessionRefreshJob = viewModelScope.launch {
            while (true) {
                val expiresAt = session.expiresAtMs()
                if (expiresAt == null) {
                    delay(ALLOHA_SESSION_EXPIRY_POLL_MS)
                    continue
                }
                delay(
                    (expiresAt - System.currentTimeMillis() - ALLOHA_SESSION_REFRESH_LEAD_MS).coerceAtLeast(
                        0L
                    )
                )
                if (activeAllohaSession === session && session.expiresAtMs() == expiresAt) {
                    session.refresh()
                }
            }
        }
    }

    private fun closeAllohaSession(immediately: Boolean = true) {
        allohaSessionRefreshJob?.cancel()
        allohaSessionRefreshJob = null
        activeAllohaSession?.let { allohaSessionManager.release(it, immediately) }
        activeAllohaSession = null
    }

    override fun onCleared() {
        allohaPlaybackRecoveryJob?.cancel()
        closeAllohaSession(immediately = false)
        streamLoadingHintJob?.cancel()
        super.onCleared()
    }

    private companion object {
        private const val ALLOHA_PLAYER_NAME = "alloha"
        private const val LOG_TAG = "PlayerViewModel"
        private const val ALLOHA_PLAYBACK_RECOVERY_DELAY_MS = 1_000L
        private const val ALLOHA_SESSION_REFRESH_LEAD_MS = 20_000L
        private const val ALLOHA_SESSION_EXPIRY_POLL_MS = 500L
        private const val CHANGE_PLAYER_HINT_DELAY_MS = 10_000L
        private const val ALLOHA_RECOVERY_HINT_DELAY_MS = 15_000L
    }

}
