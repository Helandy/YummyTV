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
import su.afk.yummy.tv.core.analytics.AnalyticsEvents
import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.settings.PlayerMobileVideoTransformSettings
import su.afk.yummy.tv.core.preferences.settings.PlayerResizeMode
import su.afk.yummy.tv.core.preferences.settings.PlayerResizeSettings
import su.afk.yummy.tv.core.preferences.settings.PlayerZoomLevel
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.player.handler.PlayerProgressContext
import su.afk.yummy.tv.feature.player.handler.PlayerProgressHandler
import su.afk.yummy.tv.feature.player.handler.PlayerSettingsHandler
import su.afk.yummy.tv.feature.player.handler.PlayerSourceSelectionHandler
import su.afk.yummy.tv.feature.player.handler.PlayerStreamHandler
import su.afk.yummy.tv.feature.player.handler.PlayerStreamResult
import su.afk.yummy.tv.feature.player.navigator.PlayerDestination
import su.afk.yummy.tv.feature.player.utils.PlayerResizeSettingsScope

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
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModelNew<PlayerState.State, PlayerState.Event, PlayerState.Effect>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(dest: PlayerDestination): PlayerViewModel
    }

    private var activeDest: PlayerDestination = dest

    fun loadDestination(newDest: PlayerDestination) {
        if (newDest == activeDest) return
        activeDest = newDest
        setState {
            destinationStateMapper.toState(
                newDest,
                autoSkipOpeningsEndings = autoSkipOpeningsEndings
            )
        }
        observeActivePlayerResizeSettings(force = true)
        observeActivePlayerMobileVideoTransformSettings(force = true)
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
        settingsHandler.autoSkipOpeningsEndings
            .onEach { enabled -> setState { copy(autoSkipOpeningsEndings = enabled) } }
            .launchIn(viewModelScope)
        observeActivePlayerResizeSettings()
        observeActivePlayerMobileVideoTransformSettings()
        loadStream()
    }

    override fun onEvent(event: PlayerState.Event) {
        when (event) {
            PlayerState.Event.Back -> nav.back()
            PlayerState.Event.RetryStream -> {
                trackPlayerAction("retry_stream")
                setState { copy(retryKey = retryKey + 1) }
                loadStream()
            }
            PlayerState.Event.RateTitle -> {
                trackPlayerAction("rate_title")
                val animeId = currentState.animeId
                if (animeId > 0) nav.navigate(detailsNavigator.getRatingDest(animeId))
            }
            is PlayerState.Event.PlaybackError -> {
                analyticsTracker.track(AnalyticsEvents.playerError(playerAnalyticsParams()))
                setState { copy(playerError = streamHandler.playbackErrorMessage(event.message)) }
            }
            PlayerState.Event.PrevEpisode -> {
                trackPlayerAction("prev_episode")
                applySourceSelection(sourceSelectionHandler.previousEpisode(currentState))
            }
            PlayerState.Event.NextEpisode -> {
                trackPlayerAction("next_episode")
                applySourceSelection(sourceSelectionHandler.nextEpisode(currentState))
            }
            is PlayerState.Event.DubbingSelected -> {
                trackPlayerAction(
                    action = "dubbing_selected",
                    params = analyticsParamsOf("index" to event.index),
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
                trackPlayerAction(
                    action = "balancer_selected",
                    params = analyticsParamsOf("index" to event.index),
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
                trackPlayerAction(
                    action = "quality_selected",
                    params = analyticsParamsOf("quality" to event.quality),
                )
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
                trackPlayerAction(
                    action = "speed_selected",
                    params = analyticsParamsOf("speed" to event.speed),
                )
                setState { copy(selectedSpeed = event.speed.coerceAtLeast(0.1f)) }
            }

            is PlayerState.Event.ResizeModeSelected -> {
                trackPlayerAction(
                    action = "resize_mode_selected",
                    params = analyticsParamsOf("mode" to event.mode.name.lowercase()),
                )
                val settings = PlayerResizeSettings(
                    resizeMode = event.mode,
                    zoomLevel = currentState.zoomLevel,
                )
                setState { copy(resizeMode = settings.resizeMode) }
                savePlayerResizeSettings(settings)
            }

            is PlayerState.Event.ZoomLevelSelected -> {
                trackPlayerAction(
                    action = "zoom_level_selected",
                    params = analyticsParamsOf("level" to event.level.name.lowercase()),
                )
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
                val position = event.positionMs.coerceAtLeast(0L)
                val duration = event.durationMs.coerceAtLeast(0L)
                setState {
                    copy(
                        resumeFromMs = position,
                        playbackPositionMs = position,
                        playbackDurationMs = duration,
                    )
                }
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

    private fun applySourceSelection(
        state: PlayerState.State?,
        sourceScopeChanged: Boolean = false,
    ) {
        if (state == null) return
        setState { state }
        if (sourceScopeChanged) {
            observeActivePlayerResizeSettings()
            observeActivePlayerMobileVideoTransformSettings()
        }
        loadStream()
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

    private fun loadStream() {
        extractionJob?.cancel()
        extractionJob = viewModelScope.launch {
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
            val pendingResume = s.dubbingResumeMs.takeIf { it >= 0L }
            when (val result = streamHandler.resolve(s, pendingResume)) {
                is PlayerStreamResult.Stream -> setState {
                    copy(
                        streamHeaders = result.headers,
                        streamQualityMap = result.qualities,
                        streamUrl = result.url,
                        resumeFromMs = result.resumeFromMs,
                        dubbingResumeMs = if (result.consumedPendingResume) -1L else dubbingResumeMs,
                    )
                }

                is PlayerStreamResult.KodikBlocked -> setState {
                    copy(kodikBlockedError = result.message)
                }

                is PlayerStreamResult.PlayerError -> setState {
                    copy(playerError = result.message)
                }
            }
        }
    }

    private fun trackPlayerAction(
        action: String,
        params: Map<String, String> = emptyMap(),
    ) {
        analyticsTracker.track(
            AnalyticsEvents.playerAction(
                action = action,
                params = playerAnalyticsParams() + params,
            )
        )
    }

    private fun playerAnalyticsParams(): Map<String, String> =
        analyticsParamsOf("anime_id" to currentState.animeId.takeIf { it > 0 })
}
