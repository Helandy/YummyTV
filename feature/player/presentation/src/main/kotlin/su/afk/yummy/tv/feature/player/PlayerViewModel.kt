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
import su.afk.yummy.tv.feature.player.navigator.PlayerDestination
import su.afk.yummy.tv.feature.player.utils.PlayerResizeSettingsScope
import su.afk.yummy.tv.feature.player.utils.activeAllDubbingNames
import su.afk.yummy.tv.feature.player.utils.activeBalancerName
import su.afk.yummy.tv.feature.player.utils.activeDubbing
import su.afk.yummy.tv.feature.player.utils.activeDubbingUrls
import su.afk.yummy.tv.feature.player.utils.activeEpisodeNumbers
import su.afk.yummy.tv.feature.player.utils.globalDubbingNames
import su.afk.yummy.tv.feature.player.utils.resolveDubbingSource

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
            PlayerState.State(
                iframeUrl = newDest.iframeUrl,
                animeTitle = newDest.animeTitle,
                episode = newDest.episode,
                playerName = newDest.playerName,
                dubbing = newDest.dubbing,
                episodeUrls = newDest.episodeUrls,
                episodeNumbers = newDest.episodeNumbers,
                episodeVideoIds = newDest.episodeVideoIds,
                screenshotUrls = newDest.screenshotUrls,
                animeId = newDest.animeId,
                posterUrl = newDest.posterUrl,
                allDubbingNames = newDest.allDubbingNames,
                allDubbingEpisodeUrls = newDest.allDubbingEpisodeUrls,
                allDubbingEpisodeNumbers = newDest.allDubbingEpisodeNumbers,
                allDubbingEpisodeVideoIds = newDest.allDubbingEpisodeVideoIds,
                allDubbingViews = newDest.allDubbingViews,
                allBalancerNames = newDest.allBalancerNames,
                allBalancerDubbingNames = newDest.allBalancerDubbingNames,
                allBalancerEpisodeUrls = newDest.allBalancerEpisodeUrls,
                allBalancerEpisodeNumbers = newDest.allBalancerEpisodeNumbers,
                allBalancerEpisodeVideoIds = newDest.allBalancerEpisodeVideoIds,
                allBalancerDubbingViews = newDest.allBalancerDubbingViews,
                episodeSkips = newDest.episodeSkips,
                allDubbingEpisodeSkips = newDest.allDubbingEpisodeSkips,
                allBalancerEpisodeSkips = newDest.allBalancerEpisodeSkips,
                balancerIndex = newDest.currentBalancerIndex,
                dubbingIndex = newDest.currentDubbingIndex,
                episodeIndex = newDest.currentEpisodeIndex,
                autoSkipOpeningsEndings = autoSkipOpeningsEndings,
            )
        }
        observeActivePlayerResizeSettings(force = true)
        observeActivePlayerMobileVideoTransformSettings(force = true)
        loadStream()
    }

    override fun createInitialState() = PlayerState.State(
        iframeUrl = dest.iframeUrl,
        animeTitle = dest.animeTitle,
        episode = dest.episode,
        playerName = dest.playerName,
        dubbing = dest.dubbing,
        episodeUrls = dest.episodeUrls,
        episodeNumbers = dest.episodeNumbers,
        episodeVideoIds = dest.episodeVideoIds,
        screenshotUrls = dest.screenshotUrls,
        animeId = dest.animeId,
        posterUrl = dest.posterUrl,
        allDubbingNames = dest.allDubbingNames,
        allDubbingEpisodeUrls = dest.allDubbingEpisodeUrls,
        allDubbingEpisodeNumbers = dest.allDubbingEpisodeNumbers,
        allDubbingEpisodeVideoIds = dest.allDubbingEpisodeVideoIds,
        allDubbingViews = dest.allDubbingViews,
        allBalancerNames = dest.allBalancerNames,
        allBalancerDubbingNames = dest.allBalancerDubbingNames,
        allBalancerEpisodeUrls = dest.allBalancerEpisodeUrls,
        allBalancerEpisodeNumbers = dest.allBalancerEpisodeNumbers,
        allBalancerEpisodeVideoIds = dest.allBalancerEpisodeVideoIds,
        allBalancerDubbingViews = dest.allBalancerDubbingViews,
        episodeSkips = dest.episodeSkips,
        allDubbingEpisodeSkips = dest.allDubbingEpisodeSkips,
        allBalancerEpisodeSkips = dest.allBalancerEpisodeSkips,
        balancerIndex = dest.currentBalancerIndex,
        dubbingIndex = dest.currentDubbingIndex,
        episodeIndex = dest.currentEpisodeIndex,
    )

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
                setState { copy(retryKey = retryKey + 1) }
                loadStream()
            }
            PlayerState.Event.RateTitle -> {
                val animeId = currentState.animeId
                if (animeId > 0) nav.navigate(detailsNavigator.getRatingDest(animeId))
            }
            is PlayerState.Event.PlaybackError -> {
                setState { copy(playerError = streamHandler.playbackErrorMessage(event.message)) }
            }
            PlayerState.Event.PrevEpisode -> {
                val idx = currentState.episodeIndex
                if (idx > 0) {
                    setState { copy(episodeIndex = idx - 1) }
                    loadStream()
                }
            }
            PlayerState.Event.NextEpisode -> {
                val s = currentState
                val urls = activeDubbingUrls(s)
                if (s.episodeIndex < urls.size - 1) {
                    setState { copy(episodeIndex = s.episodeIndex + 1) }
                    loadStream()
                }
            }
            is PlayerState.Event.DubbingSelected -> {
                val s = currentState
                val currentNum = activeEpisodeNumbers(s).getOrElse(s.episodeIndex) { "" }
                val selectedDubbing = globalDubbingNames(s).getOrElse(event.index) {
                    activeAllDubbingNames(s).getOrElse(event.index) { "" }
                }
                if (selectedDubbing.isBlank()) return
                val selection = resolveDubbingSource(
                    state = s,
                    dubbingName = selectedDubbing,
                    episodeNumber = currentNum,
                ) ?: return
                if (
                    selection.balancerIndex == s.balancerIndex &&
                    selection.dubbingIndex == s.dubbingIndex &&
                    selection.episodeIndex == s.episodeIndex
                ) return
                setState {
                    copy(
                        dubbingResumeMs = (event.currentPosMs - 3_000L).coerceAtLeast(0L),
                        balancerIndex = selection.balancerIndex,
                        dubbingIndex = selection.dubbingIndex,
                        episodeIndex = selection.episodeIndex,
                    )
                }
                observeActivePlayerResizeSettings()
                observeActivePlayerMobileVideoTransformSettings()
                loadStream()
            }
            is PlayerState.Event.BalancerSelected -> {
                val s = currentState
                if (event.index == s.balancerIndex) return
                val newDubbingNames = s.allBalancerDubbingNames.getOrElse(event.index) { emptyList() }
                val currentDubbingName = activeDubbing(s)
                val newDubbingIdx = newDubbingNames.indexOf(currentDubbingName).takeIf { it >= 0 } ?: 0
                val currentEpNum = activeEpisodeNumbers(s).getOrElse(s.episodeIndex) { s.episode }
                val newEpNums = s.allBalancerEpisodeNumbers.getOrElse(event.index) { emptyList() }
                    .getOrElse(newDubbingIdx) { emptyList() }
                val newEpisodeIdx = newEpNums.indexOf(currentEpNum).takeIf { it >= 0 } ?: 0
                setState {
                    copy(
                        dubbingResumeMs = (event.currentPosMs - 3_000L).coerceAtLeast(0L),
                        balancerIndex = event.index,
                        dubbingIndex = newDubbingIdx,
                        episodeIndex = newEpisodeIdx,
                    )
                }
                observeActivePlayerResizeSettings()
                observeActivePlayerMobileVideoTransformSettings()
                loadStream()
            }
            is PlayerState.Event.QualitySelected -> {
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
                setState { copy(selectedSpeed = event.speed.coerceAtLeast(0.1f)) }
            }

            is PlayerState.Event.ResizeModeSelected -> {
                val settings = PlayerResizeSettings(
                    resizeMode = event.mode,
                    zoomLevel = currentState.zoomLevel,
                )
                setState { copy(resizeMode = settings.resizeMode) }
                savePlayerResizeSettings(settings)
            }

            is PlayerState.Event.ZoomLevelSelected -> {
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
        val state = currentState
        return PlayerResizeSettingsScope(
            animeId = state.animeId,
            animeTitle = state.animeTitle,
            playerName = activeBalancerName(state),
        )
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
}
