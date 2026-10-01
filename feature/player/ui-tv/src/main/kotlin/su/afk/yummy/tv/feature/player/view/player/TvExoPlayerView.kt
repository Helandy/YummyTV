package su.afk.yummy.tv.feature.player.view.player

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.common.PlayerBlackBackdrop
import su.afk.yummy.tv.feature.player.common.PlayerBufferingIndicator
import su.afk.yummy.tv.feature.player.common.PlayerKeepScreenOnEffect
import su.afk.yummy.tv.feature.player.common.PlayerLifecycleEffect
import su.afk.yummy.tv.feature.player.common.PlayerListenerEffect
import su.afk.yummy.tv.feature.player.common.PlayerMediaItemEffect
import su.afk.yummy.tv.feature.player.common.PlayerProgressPollingEffect
import su.afk.yummy.tv.feature.player.common.PlayerStallWatchdogEffect
import su.afk.yummy.tv.feature.player.common.PlayerSubtitleOverlay
import su.afk.yummy.tv.feature.player.common.PlayerTrackOption
import su.afk.yummy.tv.feature.player.common.PlayerVolumeEffect
import su.afk.yummy.tv.feature.player.common.model.PlayerProgressSource
import su.afk.yummy.tv.feature.player.common.model.StepSeekDirection
import su.afk.yummy.tv.feature.player.common.model.rememberPlayerPlaybackProgressState
import su.afk.yummy.tv.feature.player.common.rememberDelayedRecoveryIndicator
import su.afk.yummy.tv.feature.player.common.rememberPlayerAutoHideController
import su.afk.yummy.tv.feature.player.common.rememberPlayerBufferingState
import su.afk.yummy.tv.feature.player.common.rememberPlayerCompletionTracker
import su.afk.yummy.tv.feature.player.common.rememberPlayerEndFlowState
import su.afk.yummy.tv.feature.player.common.rememberPlayerMediaReadyState
import su.afk.yummy.tv.feature.player.common.rememberPlayerPlaybackKey
import su.afk.yummy.tv.feature.player.common.rememberPlayerProgressReporter
import su.afk.yummy.tv.feature.player.common.rememberPlayerSeekController
import su.afk.yummy.tv.feature.player.common.rememberPlayerSkipUiState
import su.afk.yummy.tv.feature.player.common.rememberPlayerStepSeekToastState
import su.afk.yummy.tv.feature.player.common.rememberPlayerSystemVolumeController
import su.afk.yummy.tv.feature.player.common.rememberPlayerTrackMenu
import su.afk.yummy.tv.feature.player.common.rememberPlayerVolumeController
import su.afk.yummy.tv.feature.player.common.service.rememberPlayerPlaybackSessionClient
import su.afk.yummy.tv.feature.player.common.toastIcon
import su.afk.yummy.tv.feature.player.common.utils.currentSkip
import su.afk.yummy.tv.feature.player.common.utils.skipPlayerSegment
import su.afk.yummy.tv.feature.player.common.view.PlayerEndPromptCountdownEffect
import su.afk.yummy.tv.feature.player.model.PanelReturnFocusTarget
import su.afk.yummy.tv.feature.player.model.PlayerControlFocusTarget
import su.afk.yummy.tv.feature.player.model.PlayerNextEpisodeSource
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.model.TvPlayerExitState
import su.afk.yummy.tv.feature.player.model.TvPlayerPanel
import su.afk.yummy.tv.feature.player.model.rememberTvPlayerFocusRequesters
import su.afk.yummy.tv.feature.player.model.rememberTvPlayerPanelsState
import su.afk.yummy.tv.feature.player.model.rememberTvPlayerVolumeKeysState
import su.afk.yummy.tv.feature.player.utils.speedLabel
import su.afk.yummy.tv.feature.player.utils.tvPlayerContentScale
import su.afk.yummy.tv.feature.player.view.TvPlayerRecoveryHint
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@OptIn(UnstableApi::class)
@Composable
internal fun TvExoPlayerView(
    state: PlayerState.State,
    playback: PlayerPlaybackUiState,
    streamUrl: String,
    restoreControlFocusTarget: PlayerControlFocusTarget?,
    exitState: TvPlayerExitState,
    tutorialBlocksPlayback: Boolean = false,
    onControlFocusRestored: () -> Unit,
    onDubbingSelected: (dubbingIndex: Int, currentPositionMs: Long) -> Unit,
    onBalancerSelected: (balancerIndex: Int, currentPositionMs: Long) -> Unit,
    onPlayerEvent: (PlayerState.Event) -> Unit,
) {
    val context = LocalContext.current
    val episodeKey = playback.activeIframeUrl
    val speeds = remember { listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f) }
    val activeQuality = playback.activeQuality
    val activeSpeed = state.selectedSpeed.coerceAtLeast(0.1f)
    var wantsPlay by remember { mutableStateOf(true) }
    val playbackShouldPlay = wantsPlay && !tutorialBlocksPlayback
    val progress = rememberPlayerPlaybackProgressState()
    // Буфер нового стрима/серии считается с нуля (позиция приходит из polling-цикла).
    LaunchedEffect(streamUrl, episodeKey) { progress.bufferedProgress = 0f }
    var controllerVisible by remember { mutableStateOf(true) }
    val panels = rememberTvPlayerPanelsState()
    val prompts = rememberPlayerEndFlowState(episodeKey, streamUrl)
    val skipUi = rememberPlayerSkipUiState(episodeKey)
    val stepSeekToast = rememberPlayerStepSeekToastState(
        streamUrl = streamUrl,
        toastDuration = TV_PLAYER_INLINE_TOAST_DURATION,
    )
    val focus = rememberTvPlayerFocusRequesters()
    val systemVolume = rememberPlayerSystemVolumeController()
    val volumeController = rememberPlayerVolumeController()
    val advancedVolumeEnabled = state.advancedPlayerVolumeEnabled
    val playerVolumeLevel by volumeController.volume.collectAsStateWithLifecycle()
    val playerVolumePercent = (playerVolumeLevel * 100f).roundToInt()
    val volumeKeys = rememberTvPlayerVolumeKeysState(
        indicatorDuration = TV_PLAYER_INLINE_TOAST_DURATION,
    )
    // Пока виден хинт восстановления, оверлей нельзя автоскрывать:
    // иначе фокус уйдёт на скрытый key-оверлей и кнопки хинта станут недостижимы
    val recoveryHintVisible = playback.showRecoveryHint
    val autoHide = rememberPlayerAutoHideController(
        hideDelay = state.controlsAutoHideSeconds.seconds,
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

    val currentUrl = playback.playbackUrl

    val playbackSession = rememberPlayerPlaybackSessionClient()
    val player = playbackSession.player
    val isBuffering = rememberPlayerBufferingState(player)
    val showRecoveryIndicator = rememberDelayedRecoveryIndicator(state.isPlaybackRecovering)
    val playbackKey = rememberPlayerPlaybackKey(state, currentUrl)
    val isMediaReady = rememberPlayerMediaReadyState(player, playbackKey)

    val progressSource = remember(
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

    // На ТВ уход в фон означает уход с плеера: выгружаем уже на ON_STOP.
    PlayerLifecycleEffect(
        player = player,
        reporter = reporter,
        fallbackDurationMs = { progress.duration },
        wantsPlay = { playbackShouldPlay },
        keepPlayingOnPause = { false },
        keepPlayingOnLeave = { false },
        releaseOnStop = true,
        onPaused = prompts::onPaused,
        // Отключаемся от сервиса сразу: в свёрнутом приложении экран плеера не уходит из
        // композиции, и привязанный контроллер держал бы сервис с плеером в памяти до возврата.
        onRelease = { playbackSession.stopPlaybackAndService(releaseConnection = true) },
    )

    PlayerMediaItemEffect(
        player = player,
        playbackKey = playbackKey,
        state = state,
        playback = playback,
        shouldPlay = { playbackShouldPlay },
    )

    if (player == null) {
        PlayerBlackBackdrop()
        return
    }

    PlayerKeepScreenOnEffect()

    val trackMenu = rememberPlayerTrackMenu(player, state)

    // Туториал блокирует воспроизведение, не трогая wantsPlay: после закрытия видео стартует само.
    LaunchedEffect(player, playbackShouldPlay) {
        player.playWhenReady = playbackShouldPlay
    }

    LaunchedEffect(exitState.requested) {
        if (exitState.requested) {
            prompts.hideAll()
            autoHide.cancel()
            player.pause()
        }
    }
    val completionTracker = rememberPlayerCompletionTracker(
        contentKey = episodeKey,
        streamUrl = streamUrl,
        reporter = reporter,
        onEvent = onPlayerEvent,
    )

    /** Единая точка конца эпизода: STATE_ENDED, перемотка в конец и детект по позиции. */
    fun handleEpisodeEnd(positionMs: Long, durationMs: Long) {
        val promptShown = prompts.onEpisodeEnd(
            positionMs = positionMs,
            durationMs = durationMs,
            completionTracker = completionTracker,
            playback = playback,
            autoPlayNextEpisode = state.autoPlayNextEpisode,
            nextEpisodeDelaySeconds = state.nextEpisodeSwitchDelaySeconds,
            suppressPrompts = exitState.requested,
        )
        if (!promptShown) return
        controllerVisible = true
        panels.close()
        autoHide.cancel()
    }

    val seekController = rememberPlayerSeekController(
        player = player,
        progress = progress,
        reporter = reporter,
        stepSeekToast = stepSeekToast,
        onEpisodeEnd = ::handleEpisodeEnd,
        onLeftEnd = prompts::onLeftEnd,
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
        prompts.hideAll()
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

    // Позиция тикает каждые 500 мс; через derivedStateOf экран перекомпоновывается только
    // когда активная заставка реально меняется, а не на каждом тике.
    val activeSkip by remember(isMediaReady, playback.activeSkips, skipUi.dismissedSkipKeys) {
        derivedStateOf {
            if (isMediaReady) {
                currentSkip(playback.activeSkips, progress.currentPosition, skipUi.dismissedSkipKeys)
            } else {
                null
            }
        }
    }

    fun skipActiveSegment(reportSelection: Boolean = true) {
        val skip = activeSkip ?: return
        skipUi.highlightedSkipKey = null
        skipPlayerSegment(
            skip = skip,
            context = context,
            player = player,
            skipUi = skipUi,
            seekController = seekController,
            reportSelection = reportSelection,
            onEvent = onPlayerEvent,
        )
        onInteraction()
    }

    PlayerStallWatchdogEffect(player = player, onEvent = onPlayerEvent)

    PlayerListenerEffect(
        player = player,
        skipUi = skipUi,
        stepSeekToast = stepSeekToast,
        fallbackDurationMs = { progress.duration },
        wantsPlay = { playbackShouldPlay },
        onWantsPlayChanged = {
            if (!tutorialBlocksPlayback) wantsPlay = it
        },
        autoHide = { schedule -> if (schedule) autoHide.schedule() else autoHide.cancel() },
        onEpisodeEnd = { positionMs, durationMs ->
            handleEpisodeEnd(positionMs, durationMs)
        },
        onEvent = onPlayerEvent,
    )

    LaunchedEffect(player, activeSpeed) {
        player.setPlaybackSpeed(activeSpeed)
    }

    PlayerVolumeEffect(
        player = player,
        advancedVolumeEnabled = advancedVolumeEnabled,
        volumeLevel = playerVolumeLevel,
    )

    PlayerProgressPollingEffect(
        player = player,
        progress = progress,
        reporter = reporter,
        episodeKey = episodeKey,
        onPositionAtEnd = { positionMs, durationMs ->
            handleEpisodeEnd(positionMs, durationMs)
        },
    )

    TvPlayerFocusEffects(
        focus = focus,
        panels = panels,
        prompts = prompts,
        controllerVisible = controllerVisible,
        recoveryHintVisible = recoveryHintVisible,
        tutorialActive = state.tvControlsTutorialReady && state.showTvControlsTutorial,
        restoreControlFocusTarget = restoreControlFocusTarget,
        onControlFocusRestored = onControlFocusRestored,
    )

    TvPlayerAutoSkipEffect(
        activeSkip = activeSkip,
        autoSkipOpeningsEndings = state.autoSkipOpeningsEndings,
        delaySeconds = state.autoSkipDelaySeconds,
        isPlaying = playbackShouldPlay,
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
            prompts.dismissNextEpisode()
            prompts.finalEpisodeActionPrompt = null
            panels.close(
                returnFocusTarget = when (panels.activePanel) {
                    TvPlayerPanel.Quality -> PanelReturnFocusTarget.Quality
                    TvPlayerPanel.Dubbing -> PanelReturnFocusTarget.Dubbing
                    TvPlayerPanel.Balancer -> PanelReturnFocusTarget.Balancer
                    TvPlayerPanel.Speed -> PanelReturnFocusTarget.Speed
                    TvPlayerPanel.Resize -> PanelReturnFocusTarget.Resize
                    TvPlayerPanel.Volume -> PanelReturnFocusTarget.Volume
                    TvPlayerPanel.Alloha -> PanelReturnFocusTarget.Alloha
                    null -> null
                },
            )
        } else {
            autoHide.cancel()
            controllerVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                // «Продвинутая» громкость меняет внутренний уровень плеера (±1%, 0–200%),
                // иначе — системную (если включён перехват). Без обеих настроек кнопки
                // уходят системе — поведение по умолчанию не меняем.
                if (!advancedVolumeEnabled && !state.tvPlayerVolumeKeysEnabled) {
                    return@onPreviewKeyEvent false
                }
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent event.key == Key.VolumeUp ||
                        event.key == Key.VolumeDown
                }
                val up = when (event.key) {
                    Key.VolumeUp -> true
                    Key.VolumeDown -> false
                    else -> return@onPreviewKeyEvent false
                }
                if (advancedVolumeEnabled) {
                    volumeKeys.show(volumeController.stepBy(if (up) 1 else -1))
                } else {
                    val fraction = systemVolume.stepBy(if (up) 0.01f else -0.01f)
                    volumeKeys.show((fraction * 100f).roundToInt())
                }
                true
            },
    ) {
        ContentFrame(
            player = player,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            contentScale = tvPlayerContentScale(state.resizeMode, state.zoomLevel),
            keepContentOnReset = state.isPlaybackRecovering,
            shutter = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false },
        )

        PlayerSubtitleOverlay(
            player = player,
            style = state.subtitleStyle,
            modifier = Modifier.fillMaxSize(),
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
            visible = isBuffering || showRecoveryIndicator,
            modifier = Modifier.align(Alignment.Center),
        )

        if (recoveryHintVisible) {
            TvPlayerRecoveryHint(
                onChangePlayer = if (playback.canChangePlayer) {
                    { togglePanel(TvPlayerPanel.Balancer, PanelReturnFocusTarget.Balancer) }
                } else {
                    null
                },
                onChangeDubbing = if (playback.canChangeDubbing) {
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
            wantsPlay = playbackShouldPlay,
            playback = playback,
            animeTitle = state.animeTitle,
            activeSkip = activeSkip,
            autoSkipRemainingSeconds = skipUi.autoSkipRemainingSeconds(activeSkip?.key),
            autoSkipProgress = skipUi.autoSkipProgress(activeSkip?.key),
            showOpeningOnTimeline = state.showOpeningOnTimeline,
            highlightedSkipKey = skipUi.highlightedSkipKey,
            qualityCount = playback.qualityLabels.size,
            currentQualityLabel = activeQuality.orEmpty(),
            currentSpeedLabel = activeSpeed.speedLabel(),
            showVolumeButton = advancedVolumeEnabled,
            showAllohaButton = trackMenu.showAudioChoice || trackMenu.showSubtitleChoice,
            onPlayPause = { if (wantsPlay) player.pause() else player.play() },
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
            onToggleVolume = {
                togglePanel(TvPlayerPanel.Volume, PanelReturnFocusTarget.Volume)
            },
            onToggleAlloha = {
                togglePanel(TvPlayerPanel.Alloha, PanelReturnFocusTarget.Alloha)
            },
        )

        TvPlayerPanelsHost(
            panels = panels,
            focus = focus,
            playback = playback,
            qualities = playback.qualityLabels,
            activeQuality = activeQuality,
            speeds = speeds,
            activeSpeed = activeSpeed,
            resizeMode = state.resizeMode,
            zoomLevel = state.zoomLevel,
            volumePercent = playerVolumePercent,
            audioTrackNames = trackMenu.audioOptions.map(PlayerTrackOption::label),
            selectedAudioTrackIndex = trackMenu.selectedAudioIndex,
            subtitleTrackNames = trackMenu.subtitleOptions.map(PlayerTrackOption::label),
            selectedSubtitleTrackIndex = trackMenu.selectedSubtitleIndex,
            onQualitySelected = { idx ->
                val position = player.currentPosition.coerceAtLeast(0L)
                reporter.saveProgress(position, progress.duration)
                onPlayerEvent(PlayerState.Event.QualitySelected(playback.qualityLabels[idx], position))
                panels.close(PanelReturnFocusTarget.Quality)
                onInteraction()
            },
            onDubbingSelected = { idx ->
                onDubbingSelected(idx, player.currentPosition)
                panels.close(PanelReturnFocusTarget.Dubbing)
                onInteraction()
            },
            onBalancerSelected = { idx ->
                onBalancerSelected(idx, player.currentPosition)
                panels.close(PanelReturnFocusTarget.Balancer)
                onInteraction()
            },
            onSpeedSelected = { idx ->
                onPlayerEvent(PlayerState.Event.SpeedSelected(speeds[idx]))
                panels.close(PanelReturnFocusTarget.Speed)
                onInteraction()
            },
            onResizeModeSelected = { mode ->
                onPlayerEvent(PlayerState.Event.ResizeModeSelected(mode))
                onInteraction()
            },
            onZoomLevelSelected = { level ->
                if (level != state.zoomLevel || state.resizeMode != PlayerResizeMode.ZOOM) {
                    onPlayerEvent(PlayerState.Event.ZoomLevelSelected(level))
                }
                onInteraction()
            },
            onVolumeChange = { volumeController.setPercent(it) },
            onAudioTrackSelected = { idx ->
                trackMenu.selectAudio(idx, player.currentPosition.coerceAtLeast(0L), onPlayerEvent)
                panels.close(PanelReturnFocusTarget.Alloha)
                onInteraction()
            },
            onSubtitleTrackSelected = { idx ->
                trackMenu.selectSubtitle(idx, player.currentPosition.coerceAtLeast(0L), onPlayerEvent)
                panels.close(PanelReturnFocusTarget.Alloha)
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

        TvPlayerInlineToast(
            text = volumeKeys.indicatorText,
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 48.dp),
        )
    }
}
