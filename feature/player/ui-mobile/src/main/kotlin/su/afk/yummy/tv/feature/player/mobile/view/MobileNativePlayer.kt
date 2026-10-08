package su.afk.yummy.tv.feature.player.mobile.view

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.utils.cast.CastSupport
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
import su.afk.yummy.tv.feature.player.common.model.PlayerEndPromptState
import su.afk.yummy.tv.feature.player.common.model.PlayerProgressSource
import su.afk.yummy.tv.feature.player.common.model.StepSeekDirection
import su.afk.yummy.tv.feature.player.common.model.rememberPlayerPlaybackProgressState
import su.afk.yummy.tv.feature.player.common.rememberDelayedRecoveryIndicator
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
import su.afk.yummy.tv.feature.player.common.service.rememberPlayerMediaController
import su.afk.yummy.tv.feature.player.common.toastIcon
import su.afk.yummy.tv.feature.player.common.utils.currentSkip
import su.afk.yummy.tv.feature.player.common.utils.isVisible
import su.afk.yummy.tv.feature.player.common.utils.playerContentScale
import su.afk.yummy.tv.feature.player.common.utils.skipPlayerSegment
import su.afk.yummy.tv.feature.player.common.view.PlayerEndPromptCountdownEffect
import su.afk.yummy.tv.feature.player.mobile.cast.MobileCastingIndicator
import su.afk.yummy.tv.feature.player.mobile.cast.rememberMobileCastConnectionState
import su.afk.yummy.tv.feature.player.mobile.cast.stopCasting
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerKeyAction
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerSettingsMode
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerTrackSettingsTab
import su.afk.yummy.tv.feature.player.mobile.model.MobileVerticalGestureZone
import su.afk.yummy.tv.feature.player.mobile.model.MobileVideoTransform
import su.afk.yummy.tv.feature.player.mobile.model.rememberMobilePlayerGestureController
import su.afk.yummy.tv.feature.player.mobile.model.rememberMobilePlayerOverlayController
import su.afk.yummy.tv.feature.player.mobile.pip.MobilePlayerPipController
import su.afk.yummy.tv.feature.player.mobile.utils.gestureIcon
import su.afk.yummy.tv.feature.player.mobile.utils.toGesturePercentText
import su.afk.yummy.tv.feature.player.mobile.utils.toMobilePlayerKeyAction
import su.afk.yummy.tv.feature.player.mobile.view.tutorial.MobilePlayerGestureTutorial
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction
import su.afk.yummy.tv.feature.player.model.PlayerNextEpisodeSource
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.presentation.R
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@OptIn(UnstableApi::class)
@Composable
internal fun MobileNativePlayer(
    state: PlayerState.State,
    ui: PlayerPlaybackUiState,
    streamUrl: String,
    videoTransform: MobileVideoTransform,
    onVideoTransformChanged: (MobileVideoTransform) -> Unit,
    onEvent: (PlayerState.Event) -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { MobilePlayerPipController.findActivity(context) }
    val supportsPictureInPicture = remember(context) { MobilePlayerPipController.canEnter(context) }
    val isInPictureInPictureMode = MobilePlayerPipController.isInPictureInPictureMode
    val tutorialBlocksPlayback = state.mobileTutorialBlocksPlayback
    val tutorialVisible =
        state.mobileGestureTutorialReady &&
            state.showMobileGestureTutorial &&
            !isInPictureInPictureMode
    val pipSession = remember { MobilePlayerPipController.createSession() }
    val selectedQuality = ui.activeQuality
    val selectedSpeed = state.selectedSpeed
    // Новая серия/стрим начинают со значений из state (позиция возобновления).
    val progress = rememberPlayerPlaybackProgressState(
        ui.activeIframeUrl,
        streamUrl,
        initialPositionMs = state.playbackPositionMs.takeIf { it > 0L } ?: state.resumeFromMs,
        initialDurationMs = state.playbackDurationMs,
    )
    var settingsMode by remember { mutableStateOf<MobilePlayerSettingsMode?>(null) }
    // Шторка настроек дорожек открывается на том табе, который юзер выбрал в прошлый раз,
    // пока открыт этот экран плеера (и переживает поворот). Недоступный таб шторка сама
    // откатит на первый.
    var settingsTrackTab by rememberSaveable { mutableStateOf(MobilePlayerTrackSettingsTab.Dubbing) }
    var volumePanelOpen by remember { mutableStateOf(false) }
    var wantsPlay by remember { mutableStateOf(true) }
    val endFlow = rememberPlayerEndFlowState(ui.activeIframeUrl, streamUrl)
    val skipUi = rememberPlayerSkipUiState(ui.activeIframeUrl)
    val currentUrl = ui.playbackUrl
    val playbackConfigKey = rememberPlayerPlaybackKey(state, currentUrl)
    val mediaController = rememberPlayerMediaController()
    val castSupported = remember(context) { CastSupport.isSupported(context) }
    val castConnection = rememberMobileCastConnectionState()
    // Пока идёт Cast-сессия, локальный экран остаётся как есть (таймлайн/контролы видны) - сворачивать
    // в PiP незачем: видео и так уходит на приёмник, а не рендерится в локальном окне.
    SideEffect {
        pipSession.setEnabled(
            state.pictureInPictureEnabled && !tutorialBlocksPlayback && !castConnection.isCasting,
        )
    }
    val systemVolume = rememberPlayerSystemVolumeController()
    val volumeController = rememberPlayerVolumeController()
    val advancedVolumeEnabled = state.advancedPlayerVolumeEnabled
    val playerVolumeLevel by volumeController.volume.collectAsStateWithLifecycle()
    val playerVolumePercent = (playerVolumeLevel * 100f).roundToInt()

    // При выключении режима прячем регулятор; звук вернётся к системному через эффект
    // применения player.volume (100%). Сохранённый уровень при этом не сбрасываем.
    LaunchedEffect(advancedVolumeEnabled) {
        if (!advancedVolumeEnabled) volumePanelOpen = false
    }
    val stepSeekToast = rememberPlayerStepSeekToastState(
        streamUrl = streamUrl,
        toastDuration = MOBILE_PLAYER_SEEK_TOAST_DURATION,
    )
    // Пока виден хинт восстановления, оверлей не автоскрываем:
    // кнопки «Сменить плеер/озвучку» и контролы должны оставаться на экране
    val recoveryHintVisible = ui.showRecoveryHint && !isInPictureInPictureMode
    val overlay = rememberMobilePlayerOverlayController(
        hideDelay = state.controlsAutoHideSeconds.seconds,
        canHide = {
            wantsPlay &&
                settingsMode == null &&
                !progress.isSeeking &&
                !recoveryHintVisible &&
                !tutorialBlocksPlayback &&
                !castConnection.isCasting
        },
        wantsPlay = { wantsPlay },
        isPromptVisible = { endFlow.anyVisible },
    )

    LaunchedEffect(recoveryHintVisible) {
        if (recoveryHintVisible) {
            overlay.cancelHide()
            overlay.visible = true
        }
    }

    // Видео-области при касте всё равно нет (кадры уходят на приёмник) - таймлайн и контролы
    // не должны прятаться сами по себе, иначе экран останется пустым без единой подсказки.
    LaunchedEffect(castConnection.isCasting) {
        if (castConnection.isCasting) {
            overlay.cancelHide()
            overlay.visible = true
        }
    }
    val gestures = rememberMobilePlayerGestureController(
        activity = activity,
        initialTransform = videoTransform,
        volumeLevelProvider = {
            if (advancedVolumeEnabled) {
                volumeController.volume.value
            } else {
                systemVolume.currentFraction()
            }
        },
        onVolumeChanged = { level ->
            if (advancedVolumeEnabled) {
                volumeController.setPercent((level * 100f).roundToInt())
            } else {
                systemVolume.setFraction(level)
            }
        },
        onGestureStart = { overlay.cancelHide() },
        onVideoTransformChanged = onVideoTransformChanged,
    )
    val effectiveSpeed = if (gestures.isSpeedBoosted) MOBILE_PLAYER_SPEED_BOOST else selectedSpeed
    val playbackShouldPlay = wantsPlay && !tutorialBlocksPlayback
    val transformScopeKey = remember(state.animeId, state.animeTitle, ui.activeBalancerName) {
        "${state.animeId}|${state.animeTitle}|${ui.activeBalancerName}"
    }

    DisposableEffect(pipSession) {
        MobilePlayerPipController.registerSession(pipSession)
        onDispose {
            MobilePlayerPipController.unregisterSession(pipSession)
        }
    }

    PlayerKeepScreenOnEffect(
        releaseWhenEnded = state.screenOffAfterEnd,
        ended = endFlow.ended,
    )

    val player = mediaController
    val isBuffering = rememberPlayerBufferingState(player)
    val showRecoveryIndicator = rememberDelayedRecoveryIndicator(state.isPlaybackRecovering)
    val isMediaReady = rememberPlayerMediaReadyState(player, playbackConfigKey)

    PlayerMediaItemEffect(
        player = player,
        playbackKey = playbackConfigKey,
        state = state,
        playback = ui,
        shouldPlay = { playbackShouldPlay },
    )

    if (player == null) {
        PlayerBlackBackdrop()
        return
    }

    val trackMenu = rememberPlayerTrackMenu(player, state)

    LaunchedEffect(tutorialBlocksPlayback, isInPictureInPictureMode, player) {
        if (tutorialBlocksPlayback) {
            player.pause()
            overlay.visible = false
            overlay.cancelHide()
            settingsMode = null
            endFlow.hideAll()
            gestures.resetForPictureInPicture()
        }
    }

    val progressSource = remember(player, ui) {
        PlayerProgressSource(
            episodeUrl = ui.activeIframeUrl,
            episode = ui.activeEpisode,
            videoId = ui.activeVideoId,
            playerName = ui.activeBalancerName,
            dubbing = ui.activeDubbing,
            screenshotUrl = ui.activeScreenshotUrl,
        )
    }
    // Позицию в progress пишут общий поллинг и перемотка, репортер только отчитывается во VM.
    val reporter = rememberPlayerProgressReporter(
        source = { progressSource },
        onEvent = onEvent,
    )
    val completionTracker = rememberPlayerCompletionTracker(
        contentKey = ui.activeIframeUrl,
        streamUrl = streamUrl,
        reporter = reporter,
        onEvent = onEvent,
    )

    /** Единая точка конца эпизода: STATE_ENDED, перемотка в конец и детект по позиции. */
    fun handleEpisodeEnd(positionMs: Long, durationMs: Long) {
        val promptShown = endFlow.onEpisodeEnd(
            positionMs = positionMs,
            durationMs = durationMs,
            completionTracker = completionTracker,
            playback = ui,
            autoPlayNextEpisode = state.autoPlayNextEpisode,
            nextEpisodeDelaySeconds = state.nextEpisodeSwitchDelaySeconds,
            suppressPrompts = isInPictureInPictureMode,
        )
        if (!promptShown) return
        overlay.visible = false
        settingsMode = null
        overlay.cancelHide()
    }

    fun playNextEpisode() {
        reporter.saveProgress(progress.currentPosition, progress.duration)
        endFlow.hideAll()
        onEvent(PlayerState.Event.NextEpisode(PlayerNextEpisodeSource.EndPrompt))
    }

    val seekController = rememberPlayerSeekController(
        player = player,
        progress = progress,
        reporter = reporter,
        stepSeekToast = stepSeekToast,
        onEpisodeEnd = ::handleEpisodeEnd,
        onLeftEnd = endFlow::onLeftEnd,
    )

    // Объявлен раньше PlayerListenerEffect и MobilePlayerPipEffect, поэтому освобождается после них:
    // clearMediaItems()/stop() идут уже без слушателя, иначе пустой плейлист выглядит концом серии.
    PlayerLifecycleEffect(
        player = player,
        reporter = reporter,
        fallbackDurationMs = { progress.duration },
        wantsPlay = { playbackShouldPlay },
        // Каст продолжает играть на приёмнике независимо от PiP - фон телефона тут ни при чём.
        keepPlayingOnPause = {
            pipSession.shouldKeepPlayingOnPause() || castConnection.isCasting
        },
        keepPlayingOnLeave = pipSession::shouldKeepPlayingOnPause,
        releaseOnStop = false,
        onPaused = endFlow::onPaused,
        onRelease = {
            player.clearMediaItems()
            player.stop()
        },
    )

    PlayerStallWatchdogEffect(player = player, onEvent = onEvent)

    PlayerListenerEffect(
        player = player,
        skipUi = skipUi,
        stepSeekToast = stepSeekToast,
        fallbackDurationMs = { progress.duration },
        wantsPlay = { playbackShouldPlay },
        onWantsPlayChanged = {
            if (!tutorialBlocksPlayback) wantsPlay = it
        },
        autoHide = { schedule -> if (schedule) overlay.scheduleHide() else overlay.cancelHide() },
        onEpisodeEnd = ::handleEpisodeEnd,
        onEvent = onEvent,
    )
    MobilePlayerPipEffect(
        player = player,
        activity = activity,
        pipSession = pipSession,
        seekController = seekController,
    )

    LaunchedEffect(isInPictureInPictureMode) {
        if (isInPictureInPictureMode) {
            overlay.visible = false
            endFlow.hideAll()
            gestures.resetForPictureInPicture()
            settingsMode = null
            overlay.cancelHide()
            stepSeekToast.clear()
        }
    }

    LaunchedEffect(transformScopeKey) {
        gestures.endTransformGesture()
        gestures.liveVideoTransform = videoTransform
    }

    LaunchedEffect(ui.activeIframeUrl) {
        settingsMode = null
    }

    PlayerEndPromptCountdownEffect(
        promptState = endFlow.nextEpisodePrompt,
        contentKey = ui.activeIframeUrl,
        onPromptStateChange = { endFlow.nextEpisodePrompt = it },
        onFinished = ::playNextEpisode,
    )

    BackHandler(enabled = endFlow.anyVisible && !isInPictureInPictureMode) {
        endFlow.dismissNextEpisode()
        endFlow.finalEpisodeActionPrompt = null
        overlay.show()
    }

    LaunchedEffect(videoTransform) {
        if (!gestures.transformGestureActive) {
            gestures.liveVideoTransform = videoTransform
        }
    }

    LaunchedEffect(player, effectiveSpeed, playbackShouldPlay) {
        player.setPlaybackSpeed(effectiveSpeed)
        player.playWhenReady = playbackShouldPlay
        pipSession.setPlaying(playbackShouldPlay, activity)
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
        episodeKey = ui.activeIframeUrl,
        onPositionAtEnd = ::handleEpisodeEnd,
    )

    // Позиция тикает раз в секунду; через derivedStateOf экран перекомпоновывается только
    // когда активная заставка реально меняется.
    val activeSkip by remember(isMediaReady, ui.activeSkips, skipUi.dismissedSkipKeys) {
        derivedStateOf {
            if (isMediaReady) {
                currentSkip(ui.activeSkips, progress.currentPosition, skipUi.dismissedSkipKeys)
            } else {
                null
            }
        }
    }

    fun skipActiveSegment(reportSelection: Boolean) {
        val skip = activeSkip ?: return
        skipPlayerSegment(
            skip = skip,
            context = context,
            player = player,
            skipUi = skipUi,
            seekController = seekController,
            reportSelection = reportSelection,
            onEvent = onEvent,
        )
    }

    MobilePlayerAutoSkipEffect(
        activeSkip = activeSkip,
        autoSkipOpeningsEndings = state.autoSkipOpeningsEndings,
        delaySeconds = state.autoSkipDelaySeconds,
        isPlaying = playbackShouldPlay,
        skipUi = skipUi,
        onSkipActiveSegment = { skipActiveSegment(reportSelection = false) },
    )

    // Корень держит фокус, чтобы клавиатура управляла плеером без предварительного клика.
    val keyboardFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { keyboardFocusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { gestures.playerSize = it }
            .focusRequester(keyboardFocusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (isInPictureInPictureMode || tutorialBlocksPlayback) return@onKeyEvent false
                when (event.toMobilePlayerKeyAction()) {
                    MobilePlayerKeyAction.PlayPause -> {
                        if (wantsPlay) player.pause() else player.play()
                    }

                    MobilePlayerKeyAction.SeekBackward ->
                        seekController.stepSeek(StepSeekDirection.Backward)

                    MobilePlayerKeyAction.SeekForward ->
                        seekController.stepSeek(StepSeekDirection.Forward)

                    null -> return@onKeyEvent false
                }
                overlay.show()
                true
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
        ) {
            ContentFrame(
                player = player,
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                contentScale = playerContentScale(state.resizeMode, state.zoomLevel),
                keepContentOnReset = state.isPlaybackRecovering,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = gestures.liveVideoTransform.scale
                        scaleY = gestures.liveVideoTransform.scale
                        translationX = gestures.liveVideoTransform.offset.x
                        translationY = gestures.liveVideoTransform.offset.y
                    },
                shutter = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                    )
                },
            )
        }

        PlayerSubtitleOverlay(
            player = player,
            style = state.subtitleStyle,
            modifier = Modifier.fillMaxSize(),
        )

        PlayerBufferingIndicator(
            visible = isBuffering || showRecoveryIndicator,
            modifier = Modifier.align(Alignment.Center),
        )

        MobilePlayerGestureLayer(
            enabled = !isInPictureInPictureMode && !tutorialBlocksPlayback,
            onTap = { if (!castConnection.isCasting) overlay.toggle() },
            onDoubleTap = seekController::stepSeek,
            onTransformStart = gestures::startTransformGesture,
            onTransform = gestures::applyVideoTransform,
            onTransformEnd = gestures::endTransformGesture,
            onVerticalDragStart = gestures::startVerticalGesture,
            onVerticalDrag = gestures::applyVerticalGesture,
            onVerticalDragEnd = gestures::endVerticalGesture,
            onLongPressStart = gestures::startSpeedBoost,
            onLongPressEnd = gestures::endSpeedBoost,
        )

        MobilePlayerTopBar(
            title = state.animeTitle,
            episode = ui.activeEpisode,
            dubbing = ui.activeDubbing,
            playerName = ui.activeBalancerName,
            onBack = { onEvent(PlayerState.Event.Back) },
            onDetails = { onEvent(PlayerState.Event.OpenDetails) },
            onPictureInPicture = { activity?.let(pipSession::enter) },
            orientationMode = state.playerOrientationMode,
            onOrientationSelected = { onEvent(PlayerState.Event.OrientationModeSelected(it)) },
            showDetails = state.animeId > 0,
            showPictureInPicture = state.pictureInPictureEnabled &&
                supportsPictureInPicture &&
                !isInPictureInPictureMode &&
                !castConnection.isCasting,
            showCast = castSupported && !isInPictureInPictureMode,
            visible = overlay.visible && !isInPictureInPictureMode && !tutorialBlocksPlayback,
        )

        MobilePlayerOverlay(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = overlay.visible && !isInPictureInPictureMode && !tutorialBlocksPlayback,
            wantsPlay = wantsPlay,
            progress = progress,
            openingStartMs = ui.activeSkips.opening?.startMs?.takeIf { state.showOpeningOnTimeline },
            openingEndMs = ui.activeSkips.opening?.endMs?.takeIf { state.showOpeningOnTimeline },
            hasPrevEpisode = ui.hasPrevEpisode,
            hasNextEpisode = ui.hasNextEpisode,
            onPlayPause = {
                if (wantsPlay) player.pause() else player.play()
                overlay.show()
            },
            onSeekChange = { value ->
                progress.isSeeking = true
                progress.seekProgress = value
                overlay.visible = true
                overlay.cancelHide()
            },
            onSeekFinished = {
                val duration = progress.duration
                if (duration > 0) {
                    val newPosition = (progress.seekProgress * duration).toLong().coerceIn(0L, duration)
                    seekController.seekTo(newPosition)
                }
                progress.isSeeking = false
                overlay.show()
            },
            onPrevEpisode = { onEvent(PlayerState.Event.PrevEpisode) },
            onNextEpisode = {
                endFlow.hideAll()
                onEvent(PlayerState.Event.NextEpisode(PlayerNextEpisodeSource.Controls))
            },
            onTrackSettings = {
                endFlow.hideAll()
                settingsMode = MobilePlayerSettingsMode.Track
                overlay.visible = true
                overlay.cancelHide()
            },
            onPlaybackSettings = {
                endFlow.hideAll()
                settingsMode = MobilePlayerSettingsMode.Playback
                overlay.visible = true
                overlay.cancelHide()
            },
            showVolumeButton = advancedVolumeEnabled,
            onVolumeSettings = {
                volumePanelOpen = !volumePanelOpen
                overlay.visible = true
                overlay.cancelHide()
            },
        )

        val skipButtonBottomPadding by animateDpAsState(
            targetValue = if (overlay.visible && !isInPictureInPictureMode) 132.dp else 36.dp,
            label = "skipButtonBottomPadding",
        )

        MobilePlayerSkipButton(
            skip = activeSkip.takeUnless {
                isInPictureInPictureMode || tutorialBlocksPlayback
            },
            countdownSeconds = skipUi.autoSkipRemainingSeconds(activeSkip?.key),
            countdownProgress = skipUi.autoSkipProgress(activeSkip?.key),
            onClick = {
                skipActiveSegment(reportSelection = true)
                // Панель не открываем принудительно, но не даём ей скрыться по таймеру.
                if (overlay.visible) overlay.show()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = 18.dp, bottom = skipButtonBottomPadding),
        )

        if (volumePanelOpen &&
            advancedVolumeEnabled &&
            overlay.visible &&
            !isInPictureInPictureMode &&
            !tutorialBlocksPlayback
        ) {
            MobilePlayerVolumePanel(
                percent = playerVolumePercent,
                onPercentChange = { volumeController.setPercent(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 132.dp)
                    .padding(horizontal = 18.dp),
            )
        }

        MobilePlayerSeekToast(
            text = stepSeekToast.text.takeUnless {
                isInPictureInPictureMode || tutorialBlocksPlayback
            },
            icon = stepSeekToast.direction.toastIcon,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (overlay.visible && !isInPictureInPictureMode) 128.dp else 36.dp),
        )

        MobilePlayerSkipToast(
            // Тосты делят одну позицию, поэтому перемотка имеет приоритет.
            text = skipUi.snackbarText?.takeIf {
                stepSeekToast.text == null &&
                    !isInPictureInPictureMode &&
                    !tutorialBlocksPlayback
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (overlay.visible && !isInPictureInPictureMode) 128.dp else 36.dp),
        )

        MobilePlayerZoomIndicator(
            visible = gestures.transformGestureActive &&
                !isInPictureInPictureMode &&
                !tutorialBlocksPlayback,
            scale = gestures.liveVideoTransform.scale,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp),
        )

        MobilePlayerGestureIndicator(
            visible = gestures.brightnessGestureActive &&
                !isInPictureInPictureMode &&
                !tutorialBlocksPlayback,
            icon = MobileVerticalGestureZone.Brightness.gestureIcon,
            percentText = gestures.brightnessLevel.toGesturePercentText(),
            modifier = Modifier
                .align(Alignment.Center),
        )

        MobilePlayerGestureIndicator(
            visible = gestures.volumeGestureActive &&
                !isInPictureInPictureMode &&
                !tutorialBlocksPlayback,
            icon = MobileVerticalGestureZone.Volume.gestureIcon,
            percentText = gestures.volumeLevel.toGesturePercentText(),
            modifier = Modifier
                .align(Alignment.Center),
        )

        MobilePlayerSpeedBoostIndicator(
            visible = gestures.isSpeedBoosted &&
                !isInPictureInPictureMode &&
                !tutorialBlocksPlayback,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp),
        )

        if (recoveryHintVisible && !tutorialBlocksPlayback) {
            MobilePlayerRecoveryHint(
                onChangePlayer = if (ui.canChangePlayer) {
                    {
                        settingsTrackTab = MobilePlayerTrackSettingsTab.Player
                        settingsMode = MobilePlayerSettingsMode.Track
                        overlay.cancelHide()
                    }
                } else {
                    null
                },
                onChangeDubbing = if (ui.canChangeDubbing) {
                    {
                        settingsTrackTab = MobilePlayerTrackSettingsTab.Dubbing
                        settingsMode = MobilePlayerSettingsMode.Track
                        overlay.cancelHide()
                    }
                } else {
                    null
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 80.dp),
            )
        }

        if (endFlow.nextEpisodePrompt.isVisible &&
            (ui.hasNextEpisode || ui.nextEpisodeDubbing != null) &&
            !isInPictureInPictureMode &&
            !tutorialBlocksPlayback
        ) {
            MobilePlayerEndPrompt(
                title = when (val prompt = endFlow.nextEpisodePrompt) {
                    is PlayerEndPromptState.WithCountdown -> stringResource(
                        R.string.player_next_episode_prompt_countdown,
                        prompt.seconds,
                    )

                    else -> {
                        val nextEpisodeDubbing = ui.nextEpisodeDubbing
                        if (!ui.hasNextEpisode && nextEpisodeDubbing != null) {
                            stringResource(
                                R.string.player_next_episode_prompt_other_dubbing,
                                nextEpisodeDubbing,
                            )
                        } else {
                            stringResource(R.string.player_next_episode_prompt)
                        }
                    }
                },
                primaryLabel = stringResource(R.string.player_watch_next),
                stayLabel = stringResource(R.string.player_stay),
                onPrimary = ::playNextEpisode,
                onStay = {
                    endFlow.dismissNextEpisode()
                    overlay.show()
                },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        val finalAction = endFlow.finalEpisodeActionPrompt
        if (finalAction != null && !isInPictureInPictureMode && !tutorialBlocksPlayback) {
            val managesSubscriptions = finalAction == PlayerFinalEpisodeAction.ManageSubscriptions
            MobilePlayerEndPrompt(
                title = stringResource(
                    if (managesSubscriptions) {
                        R.string.player_notifications_prompt
                    } else {
                        R.string.player_rate_title_prompt
                    },
                ),
                primaryLabel = stringResource(
                    if (managesSubscriptions) {
                        R.string.player_manage_notifications
                    } else {
                        R.string.player_rate_title
                    },
                ),
                stayLabel = stringResource(R.string.player_stay),
                onPrimary = {
                    endFlow.finalEpisodeActionPrompt = null
                    onEvent(
                        if (managesSubscriptions) {
                            PlayerState.Event.ManageSubscriptions
                        } else {
                            PlayerState.Event.RateTitle
                        },
                    )
                },
                onStay = {
                    endFlow.finalEpisodeActionPrompt = null
                    overlay.show()
                },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        val activeSettingsMode = settingsMode
        if (
            activeSettingsMode != null &&
            !isInPictureInPictureMode &&
            !tutorialBlocksPlayback
        ) {
            MobilePlayerSettingsSheet(
                mode = activeSettingsMode,
                qualities = ui.qualityLabels,
                selectedQuality = selectedQuality,
                onQualitySelected = { quality ->
                    val position = player.currentPosition.coerceAtLeast(0)
                    reporter.saveProgress(position, progress.duration)
                    onEvent(PlayerState.Event.QualitySelected(quality, position))
                },
                selectedSpeed = selectedSpeed,
                onSpeedSelected = { onEvent(PlayerState.Event.SpeedSelected(it)) },
                resizeModes = PlayerResizeMode.entries,
                selectedResizeMode = state.resizeMode,
                onResizeModeSelected = { onEvent(PlayerState.Event.ResizeModeSelected(it)) },
                dubbingNames = ui.dubbingNames,
                dubbingEpisodeCounts = ui.dubbingEpisodeCounts,
                dubbingViews = ui.dubbingViews,
                dubbingSourceNames = ui.dubbingSourceNames,
                dubbingAvailability = ui.dubbingAvailability,
                selectedDubbingIndex = ui.currentDubbingIndex,
                onDubbingSelected = {
                    onEvent(
                        PlayerState.Event.DubbingSelected(
                            it,
                            player.currentPosition,
                        ),
                    )
                },
                balancerNames = ui.balancerNames,
                balancerAvailability = ui.balancerAvailability,
                selectedBalancerIndex = ui.currentBalancerIndex,
                onBalancerSelected = { index ->
                    onEvent(PlayerState.Event.BalancerSelected(index, player.currentPosition))
                },
                audioTrackNames = trackMenu.audioOptions.map(PlayerTrackOption::label),
                selectedAudioTrackIndex = trackMenu.selectedAudioIndex,
                onAudioTrackSelected = { index ->
                    trackMenu.selectAudio(index, player.currentPosition.coerceAtLeast(0L), onEvent)
                },
                showAudioSection = trackMenu.showAudioChoice,
                subtitleTrackNames = trackMenu.subtitleOptions.map(PlayerTrackOption::label),
                selectedSubtitleTrackIndex = trackMenu.selectedSubtitleIndex,
                onSubtitleTrackSelected = { index ->
                    trackMenu.selectSubtitle(index, player.currentPosition.coerceAtLeast(0L), onEvent)
                },
                showSubtitleSection = trackMenu.showSubtitleChoice,
                onDismiss = { settingsMode = null },
                initialTrackTab = settingsTrackTab,
                onTrackTabChanged = { settingsTrackTab = it },
            )
        }

        if (tutorialVisible) {
            MobilePlayerGestureTutorial(
                onDismiss = {
                    onEvent(PlayerState.Event.MobileGestureTutorialDismissed)
                },
            )
        }

        // Последний ребёнок Box - только так кнопка отключения гарантированно получает тапы
        // (иначе их перехватывает MobilePlayerGestureLayer выше по дереву). Сам индикатор не
        // перехватывает тапы вне кнопки, поэтому жесты и таймлайн/контролы под ним не глушатся.
        if (castConnection.isCasting) {
            MobileCastingIndicator(
                deviceName = castConnection.deviceName,
                onStopCasting = { stopCasting(context) },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

private const val MOBILE_PLAYER_SPEED_BOOST = 2f
