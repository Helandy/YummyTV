package su.afk.yummy.tv.feature.player.view.player

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import su.afk.yummy.tv.core.preferences.settings.PlayerResizeMode
import su.afk.yummy.tv.feature.player.PlayerNextEpisodeSource
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.common.PlayerBlackBackdrop
import su.afk.yummy.tv.feature.player.common.PlayerBufferingIndicator
import su.afk.yummy.tv.feature.player.common.PlayerEndPromptCountdownEffect
import su.afk.yummy.tv.feature.player.common.PlayerEndPromptState
import su.afk.yummy.tv.feature.player.common.PlayerKeepScreenOnEffect
import su.afk.yummy.tv.feature.player.common.PlayerProgressSource
import su.afk.yummy.tv.feature.player.common.StepSeekDirection
import su.afk.yummy.tv.feature.player.common.rememberPlayerAutoHideController
import su.afk.yummy.tv.feature.player.common.rememberPlayerBufferingState
import su.afk.yummy.tv.feature.player.common.rememberPlayerCompletionTracker
import su.afk.yummy.tv.feature.player.common.rememberPlayerMediaReadyState
import su.afk.yummy.tv.feature.player.common.rememberPlayerProgressReporter
import su.afk.yummy.tv.feature.player.common.rememberPlayerStepSeekToastState
import su.afk.yummy.tv.feature.player.common.service.PlayerMediaItemUpdater
import su.afk.yummy.tv.feature.player.common.service.rememberPlayerMediaController
import su.afk.yummy.tv.feature.player.common.service.rememberPlayerPlaybackConfig
import su.afk.yummy.tv.feature.player.common.toastIcon
import su.afk.yummy.tv.feature.player.model.PanelReturnFocusTarget
import su.afk.yummy.tv.feature.player.model.PlayerControlFocusTarget
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.model.TvPlayerExitState
import su.afk.yummy.tv.feature.player.model.TvPlayerPanel
import su.afk.yummy.tv.feature.player.model.rememberTvPlaybackProgressState
import su.afk.yummy.tv.feature.player.model.rememberTvPlayerFocusRequesters
import su.afk.yummy.tv.feature.player.model.rememberTvPlayerPanelsState
import su.afk.yummy.tv.feature.player.model.rememberTvPlayerPromptsState
import su.afk.yummy.tv.feature.player.model.rememberTvPlayerSeekController
import su.afk.yummy.tv.feature.player.model.rememberTvPlayerSkipUiState
import su.afk.yummy.tv.feature.player.utils.buildTvMediaItemKey
import su.afk.yummy.tv.feature.player.utils.buildTvPlayerMediaItemConfig
import su.afk.yummy.tv.feature.player.utils.buildTvPlayerPlaybackKey
import su.afk.yummy.tv.feature.player.utils.currentSkip
import su.afk.yummy.tv.feature.player.utils.formatTime
import su.afk.yummy.tv.feature.player.utils.speedLabel
import su.afk.yummy.tv.feature.player.utils.toPlayerSkipType
import su.afk.yummy.tv.feature.player.utils.tvPlayerContentScale
import su.afk.yummy.tv.feature.player.view.TvPlayerRecoveryHint
import su.afk.yummy.tv.feature.player.view.deriveQualityUrls

@OptIn(UnstableApi::class)
@Composable
internal fun TvExoPlayerView(
    state: PlayerState.State,
    playback: PlayerPlaybackUiState,
    streamUrl: String,
    restoreControlFocusTarget: PlayerControlFocusTarget?,
    exitState: TvPlayerExitState,
    onControlFocusRestored: () -> Unit,
    onDubbingSelected: (dubbingIndex: Int, currentPositionMs: Long) -> Unit,
    onBalancerSelected: (balancerIndex: Int, currentPositionMs: Long) -> Unit,
    onPlayerEvent: (PlayerState.Event) -> Unit,
) {
    val context = LocalContext.current
    val episodeKey = playback.activeIframeUrl
    val qualities = remember(streamUrl, state.streamQualityMap) {
        state.streamQualityMap ?: deriveQualityUrls(streamUrl)
    }
    val speeds = remember { listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f) }
    val activeQuality = state.selectedQuality?.takeIf { it in qualities }
        ?: qualities.keys.lastOrNull()
    val activeSpeed = state.selectedSpeed.coerceAtLeast(0.1f)
    var seekOnSwitch by remember(streamUrl, state.retryKey) {
        mutableLongStateOf(state.resumeFromMs)
    }
    var wantsPlay by remember { mutableStateOf(true) }
    val progress = rememberTvPlaybackProgressState()
    var bufferedProgress by remember(streamUrl, episodeKey) { mutableFloatStateOf(0f) }
    var controllerVisible by remember { mutableStateOf(true) }
    val panels = rememberTvPlayerPanelsState()
    val prompts = rememberTvPlayerPromptsState(episodeKey, streamUrl)
    val skipUi = rememberTvPlayerSkipUiState(streamUrl)
    val stepSeekToast = rememberPlayerStepSeekToastState(
        streamUrl = streamUrl,
        toastDuration = TV_PLAYER_INLINE_TOAST_DURATION,
    )
    val focus = rememberTvPlayerFocusRequesters()
    val canChangePlayer = playback.balancerNames.size > 1
    val canChangeDubbing = playback.dubbingNames.size > 1
    // Пока виден хинт восстановления, оверлей нельзя автоскрывать:
    // иначе фокус уйдёт на скрытый key-оверлей и кнопки хинта станут недостижимы
    val recoveryHintVisible = state.isAllohaPlaybackRecovering && state.showChangePlayerHint &&
            (canChangePlayer || canChangeDubbing)
    val autoHide = rememberPlayerAutoHideController(
        canHide = { !panels.isAnyOpen && !prompts.anyVisible && !recoveryHintVisible },
        onHide = { controllerVisible = false },
    )

    fun onInteraction() {
        controllerVisible = true
        when {
            panels.isAnyOpen || prompts.anyVisible || recoveryHintVisible -> autoHide.cancel()
            wantsPlay -> autoHide.schedule()
            else -> autoHide.cancel()
        }
    }

    LaunchedEffect(recoveryHintVisible) {
        if (recoveryHintVisible) autoHide.cancel()
    }

    val currentUrl = remember(streamUrl, activeQuality, qualities) {
        activeQuality?.let(qualities::get) ?: streamUrl
    }

    val exoPlayer = rememberPlayerMediaController()
    val isBuffering = rememberPlayerBufferingState(exoPlayer)
    val playbackConfig = rememberPlayerPlaybackConfig()
    val mediaItemUpdater = remember { PlayerMediaItemUpdater() }
    val playbackKey =
        remember(currentUrl, state.streamHeaders, state.offlineCacheKey, state.retryKey) {
            buildTvPlayerPlaybackKey(state = state, url = currentUrl)
        }
    val isMediaReady = rememberPlayerMediaReadyState(exoPlayer, playbackKey)
    val mediaItemKey = remember(
        playbackKey,
        state.animeTitle,
        playback.activeEpisode,
        playback.activeDubbing,
        playback.activeBalancerName,
        playback.activeScreenshotUrl,
        progress.duration,
    ) {
        buildTvMediaItemKey(
            playbackKey = playbackKey,
            animeTitle = state.animeTitle,
            playback = playback,
            durationMs = progress.duration,
        )
    }

    LaunchedEffect(exoPlayer, playbackKey, mediaItemKey, episodeKey) {
        val player = exoPlayer ?: return@LaunchedEffect
        mediaItemUpdater.update(
            player = player,
            playbackConfig = playbackConfig,
            config = buildTvPlayerMediaItemConfig(
                playbackKey = playbackKey,
                mediaItemKey = mediaItemKey,
                url = currentUrl,
                state = state,
                playback = playback,
                durationMs = progress.duration,
                playbackPositionMs = seekOnSwitch,
            ),
        )
        player.playWhenReady = wantsPlay
    }

    if (exoPlayer == null) {
        PlayerBlackBackdrop()
        return
    }

    PlayerKeepScreenOnEffect()

    LaunchedEffect(exitState.requested) {
        if (exitState.requested) {
            prompts.nextEpisodePrompt = PlayerEndPromptState.Hidden
            prompts.finalEpisodeActionPrompt = null
            autoHide.cancel()
            exoPlayer.pause()
        }
    }

    val progressSource = remember(
        exoPlayer,
        episodeKey,
        playback.activeEpisode,
        playback.activeVideoId,
        playback.activeBalancerName,
        playback.activeDubbing,
        playback.activeScreenshotUrl,
    ) {
        PlayerProgressSource(
            episodeUrl = episodeKey,
            episode = playback.activeEpisode,
            videoId = playback.activeVideoId,
            playerName = playback.activeBalancerName,
            dubbing = playback.activeDubbing,
            screenshotUrl = playback.activeScreenshotUrl,
        )
    }
    val reporter = rememberPlayerProgressReporter(
        source = { progressSource },
        onEvent = onPlayerEvent,
    )
    val completionTracker = rememberPlayerCompletionTracker(
        contentKey = episodeKey,
        streamUrl = streamUrl,
        reporter = reporter,
        onEvent = onPlayerEvent,
    )
    val seekController = rememberTvPlayerSeekController(
        player = exoPlayer,
        progress = progress,
        reporter = reporter,
        stepSeekToast = stepSeekToast,
        onBackwardStep = { prompts.nextEpisodePrompt = PlayerEndPromptState.Hidden },
    )

    fun togglePanel(panel: TvPlayerPanel, returnFocusTarget: PanelReturnFocusTarget) {
        val opened = panels.toggle(panel)
        if (!opened) panels.pendingReturnFocusTarget = returnFocusTarget
        if (opened) autoHide.cancel() else onInteraction()
    }

    fun exitPanelDown(returnFocusTarget: PanelReturnFocusTarget) {
        panels.close(returnFocusTarget)
        onInteraction()
    }

    fun playNextEpisode() {
        if (exitState.requested) return
        reporter.saveProgress(progress.currentPosition, progress.duration)
        prompts.nextEpisodePrompt = PlayerEndPromptState.Hidden
        prompts.finalEpisodeActionPrompt = null
        panels.close()
        onPlayerEvent(PlayerState.Event.NextEpisode(PlayerNextEpisodeSource.EndPrompt))
    }

    fun rateTitle() {
        if (exitState.requested) return
        prompts.finalEpisodeActionPrompt = null
        panels.close()
        onPlayerEvent(PlayerState.Event.RateTitle)
    }

    fun manageSubscriptions() {
        if (exitState.requested) return
        prompts.finalEpisodeActionPrompt = null
        panels.close()
        onPlayerEvent(PlayerState.Event.ManageSubscriptions)
    }

    val activeSkip = if (isMediaReady) {
        currentSkip(playback.activeSkips, progress.currentPosition, skipUi.dismissedSkipKeys)
    } else {
        null
    }

    fun skipActiveSegment(reportSelection: Boolean = true) {
        val skip = activeSkip ?: return
        if (skip.key !in skipUi.dismissedSkipKeys) skipUi.dismissedSkipKeys += skip.key
        skipUi.highlightedSkipKey = null
        val message = context.getString(
            skip.type.skippedMessageRes,
            formatTime(skip.segment.startMs),
            formatTime(skip.segment.endMs),
        )
        skipUi.showSnackbar(message)
        val fromPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
        if (reportSelection) {
            onPlayerEvent(
                PlayerState.Event.SkipSegmentSelected(
                    type = skip.type.toPlayerSkipType(),
                    fromMs = fromPosition,
                    toMs = skip.segment.endMs,
                )
            )
        }
        seekController.seekTo(skip.segment.endMs)
        onInteraction()
    }

    TvPlayerLifecycleEffect(
        player = exoPlayer,
        reporter = reporter,
        prompts = prompts,
        fallbackDurationMs = { progress.duration },
        wantsPlay = { wantsPlay },
    )

    TvPlayerListenerEffect(
        player = exoPlayer,
        reporter = reporter,
        completionTracker = completionTracker,
        autoHide = autoHide,
        skipUi = skipUi,
        stepSeekToast = stepSeekToast,
        panels = panels,
        prompts = prompts,
        exitState = exitState,
        fallbackDurationMs = { progress.duration },
        hasNextEpisode = { playback.hasNextEpisode || playback.nextEpisodeDubbing != null },
        nextEpisodeSwitchesDubbing = {
            !playback.hasNextEpisode && playback.nextEpisodeDubbing != null
        },
        finalEpisodeAction = { playback.finalEpisodeAction },
        autoPlayNextEpisode = { state.autoPlayNextEpisode },
        wantsPlay = { wantsPlay },
        onWantsPlayChanged = { wantsPlay = it },
        onControllerVisibleChange = { controllerVisible = it },
        onEvent = onPlayerEvent,
    )

    LaunchedEffect(exoPlayer, activeSpeed) {
        exoPlayer.setPlaybackSpeed(activeSpeed)
    }

    TvPlayerProgressPollingEffect(
        player = exoPlayer,
        progress = progress,
        reporter = reporter,
        episodeKey = { episodeKey },
        onBufferedProgressChange = { bufferedProgress = it },
    )

    TvPlayerFocusEffects(
        focus = focus,
        panels = panels,
        prompts = prompts,
        controllerVisible = controllerVisible,
        recoveryHintVisible = recoveryHintVisible,
        restoreControlFocusTarget = restoreControlFocusTarget,
        onControlFocusRestored = onControlFocusRestored,
    )

    TvPlayerAutoSkipEffect(
        activeSkip = activeSkip,
        autoSkipOpeningsEndings = state.autoSkipOpeningsEndings,
        skipUi = skipUi,
        focus = focus,
        autoHide = autoHide,
        onControllerVisibleChange = { controllerVisible = it },
        onSkipActiveSegment = { reportSelection -> skipActiveSegment(reportSelection) },
    )

    PlayerEndPromptCountdownEffect(
        promptState = prompts.nextEpisodePrompt,
        contentKey = episodeKey,
        onPromptStateChange = { prompts.nextEpisodePrompt = it },
        onFinished = {
            if (!exitState.requested) playNextEpisode()
        },
    )

    BackHandler(enabled = panels.isAnyOpen || prompts.anyVisible || controllerVisible) {
        if (panels.isAnyOpen || prompts.anyVisible) {
            prompts.nextEpisodePrompt = PlayerEndPromptState.Hidden
            prompts.finalEpisodeActionPrompt = null
            panels.close(
                returnFocusTarget = when (panels.activePanel) {
                    TvPlayerPanel.Quality -> PanelReturnFocusTarget.Quality
                    TvPlayerPanel.Dubbing -> PanelReturnFocusTarget.Dubbing
                    TvPlayerPanel.Balancer -> PanelReturnFocusTarget.Balancer
                    TvPlayerPanel.Speed -> PanelReturnFocusTarget.Speed
                    TvPlayerPanel.Resize -> PanelReturnFocusTarget.Resize
                    null -> null
                }
            )
        } else {
            autoHide.cancel()
            controllerVisible = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ContentFrame(
            player = exoPlayer,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            contentScale = tvPlayerContentScale(state.resizeMode, state.zoomLevel),
            keepContentOnReset = state.isAllohaPlaybackRecovering,
            shutter = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false },
        )

        TvPlayerPointerOverlay(
            enabled = !panels.isAnyOpen && !prompts.anyVisible && !recoveryHintVisible,
            onClick = {
                if (controllerVisible) {
                    autoHide.cancel()
                    controllerVisible = false
                } else {
                    onInteraction()
                }
            },
        )

        PlayerBufferingIndicator(
            visible = isBuffering || state.isAllohaPlaybackRecovering,
            modifier = Modifier.align(Alignment.Center),
        )

        if (recoveryHintVisible) {
            TvPlayerRecoveryHint(
                onChangePlayer = if (canChangePlayer) {
                    { togglePanel(TvPlayerPanel.Balancer, PanelReturnFocusTarget.Balancer) }
                } else {
                    null
                },
                onChangeDubbing = if (canChangeDubbing) {
                    { togglePanel(TvPlayerPanel.Dubbing, PanelReturnFocusTarget.Dubbing) }
                } else {
                    null
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 100.dp),
            )
        }

        if (!controllerVisible) {
            TvPlayerHiddenKeyOverlay(
                focusRequester = focus.overlay,
                onSeekBackward = { seekController.stepSeek(StepSeekDirection.Backward) },
                onSeekForward = { seekController.stepSeek(StepSeekDirection.Forward) },
                onInteraction = ::onInteraction,
            )
        }

        TvPlayerInfoBar(
            visible = controllerVisible,
            animeTitle = state.animeTitle,
            episode = playback.activeEpisode,
            dubbing = playback.activeDubbing,
            modifier = Modifier.align(Alignment.TopStart),
        )

        TvPlayerNameBadge(
            visible = controllerVisible,
            playerName = playback.activeBalancerName,
            modifier = Modifier.align(Alignment.TopEnd),
        )

        TvPlayerControlsOverlay(
            visible = controllerVisible,
            focus = focus,
            progress = progress,
            bufferedProgress = bufferedProgress,
            wantsPlay = wantsPlay,
            playback = playback,
            animeTitle = state.animeTitle,
            activeSkip = activeSkip,
            autoSkipOpeningsEndings = state.autoSkipOpeningsEndings,
            highlightedSkipKey = skipUi.highlightedSkipKey,
            qualityCount = qualities.size,
            currentQualityLabel = activeQuality.orEmpty(),
            currentSpeedLabel = activeSpeed.speedLabel(),
            onPlayPause = { if (wantsPlay) exoPlayer.pause() else exoPlayer.play() },
            onSeekTo = seekController::seekTo,
            onInteraction = ::onInteraction,
            onSkipActiveSegment = { skipActiveSegment() },
            onPrevEpisode = { onPlayerEvent(PlayerState.Event.PrevEpisode) },
            onNextEpisode = {
                onPlayerEvent(PlayerState.Event.NextEpisode(PlayerNextEpisodeSource.Controls))
            },
            onRateTitle = ::rateTitle,
            onManageSubscriptions = ::manageSubscriptions,
            onToggleQuality = {
                togglePanel(TvPlayerPanel.Quality, PanelReturnFocusTarget.Quality)
            },
            onToggleDubbing = {
                togglePanel(TvPlayerPanel.Dubbing, PanelReturnFocusTarget.Dubbing)
            },
            onToggleBalancer = {
                togglePanel(TvPlayerPanel.Balancer, PanelReturnFocusTarget.Balancer)
            },
            onToggleResize = {
                togglePanel(TvPlayerPanel.Resize, PanelReturnFocusTarget.Resize)
            },
            onToggleSpeed = {
                togglePanel(TvPlayerPanel.Speed, PanelReturnFocusTarget.Speed)
            },
        )

        TvPlayerPanelsHost(
            panels = panels,
            focus = focus,
            playback = playback,
            qualities = qualities.keys.toList(),
            activeQuality = activeQuality,
            speeds = speeds,
            activeSpeed = activeSpeed,
            resizeMode = state.resizeMode,
            zoomLevel = state.zoomLevel,
            onQualitySelected = { idx ->
                val quality = qualities.keys.toList()[idx]
                if (quality != activeQuality) {
                    val position = exoPlayer.currentPosition.coerceAtLeast(0L)
                    seekOnSwitch = position
                    reporter.saveProgress(position, progress.duration)
                    onPlayerEvent(PlayerState.Event.QualitySelected(quality, position))
                }
                panels.close(PanelReturnFocusTarget.Quality)
                onInteraction()
            },
            onDubbingSelected = { idx ->
                onDubbingSelected(idx, exoPlayer.currentPosition)
                panels.close(PanelReturnFocusTarget.Dubbing)
                onInteraction()
            },
            onBalancerSelected = { idx ->
                onBalancerSelected(idx, exoPlayer.currentPosition)
                panels.close(PanelReturnFocusTarget.Balancer)
                onInteraction()
            },
            onSpeedSelected = { idx ->
                val speed = speeds[idx]
                if (speed != activeSpeed) onPlayerEvent(PlayerState.Event.SpeedSelected(speed))
                panels.close(PanelReturnFocusTarget.Speed)
                onInteraction()
            },
            onResizeModeSelected = { mode ->
                if (mode != state.resizeMode) {
                    onPlayerEvent(PlayerState.Event.ResizeModeSelected(mode))
                }
                onInteraction()
            },
            onZoomLevelSelected = { level ->
                if (level != state.zoomLevel || state.resizeMode != PlayerResizeMode.ZOOM) {
                    onPlayerEvent(PlayerState.Event.ZoomLevelSelected(level))
                }
                onInteraction()
            },
            onExitPanelDown = ::exitPanelDown,
        )

        TvPlayerSkipSnackbar(
            text = skipUi.snackbarText,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (controllerVisible) 136.dp else 36.dp),
        )

        TvPlayerEndPrompts(
            prompts = prompts,
            focus = focus,
            hasNextEpisode = playback.hasNextEpisode,
            nextEpisodeDubbing = playback.nextEpisodeDubbing,
            onPlayNextEpisode = ::playNextEpisode,
            onRateTitle = ::rateTitle,
            onManageSubscriptions = ::manageSubscriptions,
            onInteraction = ::onInteraction,
        )

        TvPlayerInlineToast(
            text = stepSeekToast.text,
            icon = stepSeekToast.direction.toastIcon,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (controllerVisible) 136.dp else 36.dp),
        )
    }
}
