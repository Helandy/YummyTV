package su.afk.yummy.tv.feature.player

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.settings.PlayerMobileVideoTransformSettings
import su.afk.yummy.tv.core.preferences.settings.PlayerResizeMode
import su.afk.yummy.tv.core.preferences.settings.PlayerResizeSettings
import su.afk.yummy.tv.core.preferences.settings.PlayerZoomLevel
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.player.handler.PlayerPlaybackProgressHandler
import su.afk.yummy.tv.feature.player.handler.PlayerSettingsHandler
import su.afk.yummy.tv.feature.player.handler.PlayerSourceGraphLoadResult
import su.afk.yummy.tv.feature.player.handler.PlayerSourceSelectionHandler
import su.afk.yummy.tv.feature.player.handler.PlayerSourceStreamHandler
import su.afk.yummy.tv.feature.player.handler.PlayerStreamLoadResult
import su.afk.yummy.tv.feature.player.handler.PlayerStreamResumeMode
import su.afk.yummy.tv.feature.player.navigator.PlayerDestination
import su.afk.yummy.tv.feature.player.utils.PlayerResizeSettingsScope
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
    private val analytics: PlayerAnalytics,
) : BaseViewModelNew<PlayerState.State, PlayerState.Event, PlayerState.Effect>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(dest: PlayerDestination): PlayerViewModel
    }

    private var activeDest: PlayerDestination = dest
    private var pendingDestinationResumeMs: Long? = dest.resumeFromMs.takeIf { it > 0L }
    private var sourceGraphJob: Job? = null

    fun loadDestination(newDest: PlayerDestination) {
        if (newDest == activeDest) return
        activeDest = newDest
        pendingDestinationResumeMs = newDest.resumeFromMs.takeIf { it > 0L }
        setState {
            destinationStateMapper.toState(
                newDest,
                autoSkipOpeningsEndings = autoSkipOpeningsEndings
            )
        }
        observeActivePlayerResizeSettings(force = true)
        observeActivePlayerMobileVideoTransformSettings(force = true)
        loadSourceGraph()
        loadStream()
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
        observeActivePlayerResizeSettings()
        observeActivePlayerMobileVideoTransformSettings()
        loadSourceGraph()
        loadStream()
    }

    override fun onEvent(event: PlayerState.Event) {
        when (event) {
            PlayerState.Event.Back -> saveCurrentProgressThenNavigate { nav.back() }
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
                setState { copy(retryKey = retryKey + 1) }
                refreshSourceGraphThenLoadStream()
            }

            PlayerState.Event.RateTitle -> {
                analytics.eventRateTitle(currentState.animeId)
                val animeId = currentState.animeId
                if (animeId > 0) {
                    saveCurrentProgressThenNavigate {
                        nav.navigate(detailsNavigator.getRatingDest(animeId))
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
                setState {
                    copy(playerError = sourceStreamHandler.playbackErrorMessage(event.message))
                }
            }

            PlayerState.Event.PrevEpisode -> {
                analytics.eventPrevEpisode(currentState.animeId)
                applySourceSelection(
                    sourceSelectionHandler.previousEpisode(currentState),
                    resumeMode = PlayerStreamResumeMode.SelectedSourceOnly,
                    refreshSourcesBeforeStream = true,
                )
            }

            is PlayerState.Event.NextEpisode -> {
                analytics.eventNextEpisode(currentState, event.source)
                val nextState = sourceSelectionHandler.nextEpisode(currentState)
                applySourceSelection(
                    nextState,
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

    private fun saveCurrentProgressThenNavigate(navigate: () -> Unit) {
        val request = playbackProgressHandler.currentProgressSaveRequest(currentState)
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
    ) {
        if (resumeMode == PlayerStreamResumeMode.SelectedSourceOnly) {
            pendingDestinationResumeMs = null
        }
        extractionJob?.cancel()
        extractionJob = viewModelScope.launch {
            val canPreserveCurrent = resumeMode == PlayerStreamResumeMode.PreserveCurrent
            val stateResumeMs = currentState.resumeFromMs.takeIf { canPreserveCurrent && it > 0L }
            val destinationResumeMs = pendingDestinationResumeMs.takeIf { canPreserveCurrent }
            setState { sourceStreamHandler.preparingStreamResolve(this) }
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
            )) {
                is PlayerStreamLoadResult.RefreshSources -> {
                    refreshSourceGraphThenLoadStream(result.resumeMode)
                }

                is PlayerStreamLoadResult.State -> {
                    if (result.consumedDestinationResume) {
                        pendingDestinationResumeMs = null
                    }
                    setState {
                        copy(
                            streamUrl = result.state.streamUrl,
                            streamHeaders = result.state.streamHeaders,
                            streamQualityMap = result.state.streamQualityMap,
                            playerError = result.state.playerError,
                            kodikBlockedError = result.state.kodikBlockedError,
                            resumeFromMs = result.state.resumeFromMs,
                            dubbingResumeMs = result.state.dubbingResumeMs,
                        )
                    }
                }
            }
        }
    }

}
