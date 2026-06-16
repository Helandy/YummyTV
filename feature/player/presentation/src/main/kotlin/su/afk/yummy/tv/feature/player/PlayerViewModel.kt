package su.afk.yummy.tv.feature.player

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.player.usecase.GetPlayerSourceGraphUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.player.handler.PlayerProgressContext
import su.afk.yummy.tv.feature.player.handler.PlayerProgressHandler
import su.afk.yummy.tv.feature.player.handler.PlayerSettingsHandler
import su.afk.yummy.tv.feature.player.handler.PlayerSourceSelectionHandler
import su.afk.yummy.tv.feature.player.handler.PlayerStreamHandler
import su.afk.yummy.tv.feature.player.handler.PlayerStreamResult
import su.afk.yummy.tv.feature.player.navigator.PlayerDestination
import su.afk.yummy.tv.feature.player.utils.PlayerResizeSettingsScope
import su.afk.yummy.tv.feature.player.utils.activeBalancerName
import su.afk.yummy.tv.feature.player.utils.activeDubbingName
import su.afk.yummy.tv.feature.player.utils.activeEpisode
import su.afk.yummy.tv.feature.player.utils.activeIframeUrl
import su.afk.yummy.tv.feature.player.utils.activeScreenshotUrl
import su.afk.yummy.tv.feature.player.utils.activeVideoId
import su.afk.yummy.tv.feature.player.utils.toPresentationSourceGraph

private data class PlayerCompletionAnalyticsKey(
    val animeId: Int,
    val videoId: Int,
    val episode: String,
    val iframeUrl: String,
)

private enum class PlayerStreamResumeMode {
    PreserveCurrent,
    SelectedSourceOnly,
}

@HiltViewModel(assistedFactory = PlayerViewModel.Factory::class)
class PlayerViewModel @AssistedInject internal constructor(
    @Assisted private val dest: PlayerDestination,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val streamHandler: PlayerStreamHandler,
    private val progressHandler: PlayerProgressHandler,
    private val settingsHandler: PlayerSettingsHandler,
    private val destinationStateMapper: PlayerDestinationStateMapper,
    private val sourceSelectionHandler: PlayerSourceSelectionHandler,
    private val getPlayerSourceGraph: GetPlayerSourceGraphUseCase,
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
    private val completedAnalyticsSources = mutableSetOf<PlayerCompletionAnalyticsKey>()
    private val fullyCompletedAnalyticsSources = mutableSetOf<PlayerCompletionAnalyticsKey>()

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
                loadStream()
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
                setState { copy(playerError = streamHandler.playbackErrorMessage(event.message)) }
            }

            PlayerState.Event.PrevEpisode -> {
                analytics.eventPrevEpisode(currentState.animeId)
                applySourceSelection(
                    sourceSelectionHandler.previousEpisode(currentState),
                    resumeMode = PlayerStreamResumeMode.SelectedSourceOnly,
                )
            }

            is PlayerState.Event.NextEpisode -> {
                analytics.eventNextEpisode(currentState, event.source)
                val nextState = sourceSelectionHandler.nextEpisode(currentState)
                applySourceSelection(
                    nextState,
                    resumeMode = PlayerStreamResumeMode.SelectedSourceOnly,
                )
                nextState?.let(::saveContinueTarget)
            }

            is PlayerState.Event.EpisodeCompleted -> {
                if (!isActivePlaybackSource(event.episodeUrl)) return
                reportEpisodeFullyCompleted(event.positionMs, event.durationMs)
                reportEpisodeCompletedIfWatched(event.positionMs, event.durationMs)
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
                reportEpisodeCompletedIfWatched(position, duration)
            }

            is PlayerState.Event.SeekPerformed -> Unit

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
                    progressHandler.saveProgress(
                        context = PlayerProgressContext(
                            animeId = s.animeId,
                            animeTitle = s.animeTitle,
                            posterUrl = s.posterUrl,
                        ),
                        snapshot = snapshot,
                    )
                }
            }
        }
    }

    private fun reportEpisodeCompletedIfWatched(positionMs: Long, durationMs: Long) {
        val position = positionMs.coerceAtLeast(0L)
        val duration = durationMs.coerceAtLeast(0L)
        if (!WatchProgressStore.isWatchedProgress(position, duration)) return

        val state = currentState
        val key = state.completionAnalyticsKey() ?: return
        if (!completedAnalyticsSources.add(key)) return

        analytics.eventEpisodeCompleted(
            state = state,
            positionMs = position,
            durationMs = duration,
        )
        saveWatchedProgress(state, position, duration)
    }

    private fun saveCurrentProgressThenNavigate(navigate: () -> Unit) {
        val state = currentState
        val snapshot = state.progressSnapshot(
            positionMs = state.playbackPositionMs,
            durationMs = state.playbackDurationMs,
        )
        if (snapshot == null) {
            navigate()
            return
        }

        viewModelScope.launch {
            runCatching {
                progressHandler.saveProgress(
                    context = state.progressContext(),
                    snapshot = snapshot,
                    forceRemoteSync = true,
                )
            }
            navigate()
        }
    }

    private fun saveContinueTarget(state: PlayerState.State) {
        val snapshot = state.continueTargetSnapshot() ?: return
        if (snapshot.episode.isFirstEpisodeNumber()) return
        viewModelScope.launch {
            progressHandler.saveContinueTarget(
                context = PlayerProgressContext(
                    animeId = state.animeId,
                    animeTitle = state.animeTitle,
                    posterUrl = state.posterUrl,
                ),
                snapshot = snapshot,
            )
        }
    }

    private fun String.isFirstEpisodeNumber(): Boolean {
        val normalized = trim().replace(',', '.')
        val number = normalized.toDoubleOrNull()
            ?: Regex("""\d+(?:[.,]\d+)?""")
                .find(normalized)
                ?.value
                ?.replace(',', '.')
                ?.toDoubleOrNull()
        return number == 1.0
    }

    private fun PlayerState.State.continueTargetSnapshot(): PlayerProgressSnapshot? {
        return progressSnapshot(positionMs = 0L, durationMs = 0L)
    }

    private fun saveWatchedProgress(
        state: PlayerState.State,
        positionMs: Long,
        durationMs: Long,
    ) {
        val snapshot = state.progressSnapshot(positionMs, durationMs) ?: return
        viewModelScope.launch {
            progressHandler.saveProgress(
                context = state.progressContext(),
                snapshot = snapshot,
            )
        }
    }

    private fun PlayerState.State.progressContext(): PlayerProgressContext =
        PlayerProgressContext(
            animeId = animeId,
            animeTitle = animeTitle,
            posterUrl = posterUrl,
        )

    private fun PlayerState.State.progressSnapshot(
        positionMs: Long,
        durationMs: Long,
    ): PlayerProgressSnapshot? {
        val episodeUrl = activeIframeUrl(this)
        val episode = activeEpisode(this)
        if (episodeUrl.isBlank() || episode.isBlank()) return null
        return PlayerProgressSnapshot(
            episode = episode,
            episodeUrl = episodeUrl,
            videoId = activeVideoId(this),
            playerName = activeBalancerName(this),
            dubbing = activeDubbingName(this),
            screenshotUrl = activeScreenshotUrl(this),
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    private fun reportEpisodeFullyCompleted(positionMs: Long, durationMs: Long) {
        val state = currentState
        val key = state.completionAnalyticsKey() ?: return
        if (!fullyCompletedAnalyticsSources.add(key)) return

        analytics.eventEpisodeFullyCompleted(
            state = state,
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = durationMs.coerceAtLeast(0L),
        )
    }

    private fun PlayerState.State.completionAnalyticsKey(): PlayerCompletionAnalyticsKey? {
        val animeId = animeId.takeIf { it > 0 } ?: return null
        val videoId = activeVideoId(this)
        val episode = activeEpisode(this)
        val iframeUrl = activeIframeUrl(this)
        if (videoId <= 0 && episode.isBlank() && iframeUrl.isBlank()) return null
        return PlayerCompletionAnalyticsKey(
            animeId = animeId,
            videoId = videoId,
            episode = episode,
            iframeUrl = iframeUrl,
        )
    }

    private fun applySourceSelection(
        state: PlayerState.State?,
        sourceScopeChanged: Boolean = false,
        resumeMode: PlayerStreamResumeMode = PlayerStreamResumeMode.PreserveCurrent,
    ) {
        if (state == null) return
        setState { state.preparingStreamLoad(resumeMode) }
        if (sourceScopeChanged) {
            observeActivePlayerResizeSettings()
            observeActivePlayerMobileVideoTransformSettings()
        }
        loadStream(resumeMode)
    }

    private fun PlayerState.State.preparingStreamLoad(
        resumeMode: PlayerStreamResumeMode,
    ): PlayerState.State {
        val preservePlaybackPosition = resumeMode == PlayerStreamResumeMode.PreserveCurrent
        return copy(
            streamUrl = null,
            streamHeaders = emptyMap(),
            streamQualityMap = null,
            playerError = null,
            kodikBlockedError = null,
            dubbingResumeMs = if (preservePlaybackPosition) dubbingResumeMs else -1L,
            resumeFromMs = if (preservePlaybackPosition) resumeFromMs else 0L,
            playbackPositionMs = if (preservePlaybackPosition) playbackPositionMs else 0L,
            playbackDurationMs = if (preservePlaybackPosition) playbackDurationMs else 0L,
        )
    }

    private fun isActivePlaybackSource(episodeUrl: String): Boolean =
        episodeUrl.isBlank() || episodeUrl == activeIframeUrl(currentState)

    private fun loadSourceGraph() {
        val destination = activeDest
        val request = destinationStateMapper.toSourceRequest(destination)
        sourceGraphJob?.cancel()
        if (request.animeId <= 0) return

        sourceGraphJob = viewModelScope.launch {
            val sourceGraph = try {
                getPlayerSourceGraph(request).toPresentationSourceGraph()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Throwable) {
                return@launch
            }
            if (destination != activeDest || sourceGraph.balancers.isEmpty()) return@launch

            val previousIframeUrl = activeIframeUrl(currentState)
            setState {
                val nextState = copy(
                    sourceGraph = sourceGraph,
                    sourceSelection = sourceGraph.selection,
                )
                if (activeIframeUrl(nextState) != previousIframeUrl) {
                    nextState.preparingStreamLoad(PlayerStreamResumeMode.PreserveCurrent)
                } else {
                    nextState
                }
            }
            observeActivePlayerResizeSettings()
            observeActivePlayerMobileVideoTransformSettings()
            if (activeIframeUrl(currentState) != previousIframeUrl) {
                loadStream()
            }
        }
    }

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

    private fun savePlayerResizeSettings(settings: PlayerResizeSettings) {
        val scope = currentPlayerResizeSettingsScope()
        viewModelScope.launch {
            settingsHandler.saveResizeSettings(scope, settings)
        }
    }

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

    private fun savePlayerMobileVideoTransformSettings(settings: PlayerMobileVideoTransformSettings) {
        val scope = currentPlayerResizeSettingsScope()
        playerMobileVideoTransformSaveJob?.cancel()
        playerMobileVideoTransformSaveJob = viewModelScope.launch {
            settingsHandler.saveMobileVideoTransformSettings(scope, settings)
        }
    }

    private fun currentPlayerResizeSettingsScope(): PlayerResizeSettingsScope {
        return sourceSelectionHandler.resizeSettingsScope(currentState)
    }

    private fun loadStream(
        resumeMode: PlayerStreamResumeMode = PlayerStreamResumeMode.PreserveCurrent,
    ) {
        if (resumeMode == PlayerStreamResumeMode.SelectedSourceOnly) {
            pendingDestinationResumeMs = null
        }
        extractionJob?.cancel()
        extractionJob = viewModelScope.launch {
            val canPreserveCurrent = resumeMode == PlayerStreamResumeMode.PreserveCurrent
            val stateResumeMs = currentState.resumeFromMs.takeIf { canPreserveCurrent && it > 0L }
            val destinationResumeMs = pendingDestinationResumeMs.takeIf { canPreserveCurrent }
            setState {
                copy(
                    streamUrl = null,
                    streamHeaders = emptyMap(),
                    streamQualityMap = null,
                    playerError = null,
                    kodikBlockedError = null,
                    resumeFromMs = 0L,
                    playbackPositionMs = 0L,
                    playbackDurationMs = 0L,
                )
            }
            val s = currentState
            val pendingResume = s.dubbingResumeMs.takeIf { canPreserveCurrent && it >= 0L }
                ?: destinationResumeMs
                ?: stateResumeMs
            val result = try {
                streamHandler.resolve(s, pendingResume)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                analytics.eventStreamResolveFailed(
                    state = s,
                    reason = PlayerStreamResult.REASON_EXCEPTION,
                    throwable = exception,
                )
                setState {
                    copy(
                        streamUrl = null,
                        streamHeaders = emptyMap(),
                        streamQualityMap = null,
                        playerError = streamHandler.playbackErrorMessage(
                            exception.localizedMessage ?: exception.message.orEmpty()
                        ),
                    )
                }
                return@launch
            }
            when (result) {
                is PlayerStreamResult.Stream -> {
                    if (destinationResumeMs != null && pendingResume == destinationResumeMs) {
                        pendingDestinationResumeMs = null
                    }
                    analytics.eventStreamStarted(s)
                    setState {
                        copy(
                            streamHeaders = result.headers,
                            streamQualityMap = result.qualities,
                            streamUrl = result.url,
                            resumeFromMs = result.resumeFromMs,
                            dubbingResumeMs = if (result.consumedPendingResume) -1L else dubbingResumeMs,
                        )
                    }
                }

                is PlayerStreamResult.KodikBlocked -> {
                    analytics.eventStreamResolveFailed(
                        state = s,
                        reason = PlayerStreamResult.REASON_KODIK_BLOCKED,
                        message = result.message,
                    )
                    setState {
                        copy(kodikBlockedError = result.message)
                    }
                }

                is PlayerStreamResult.PlayerError -> {
                    analytics.eventStreamResolveFailed(
                        state = s,
                        reason = result.reason,
                        message = result.message,
                    )
                    setState {
                        copy(playerError = result.message)
                    }
                }
            }
        }
    }

}
