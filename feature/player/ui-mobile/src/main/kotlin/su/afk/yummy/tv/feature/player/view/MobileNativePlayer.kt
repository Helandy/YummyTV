package su.afk.yummy.tv.feature.player.view

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import su.afk.yummy.tv.core.utils.resolveContinueWatchingImage
import su.afk.yummy.tv.feature.player.PlayerNextEpisodeSource
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.common.PlayerBlackBackdrop
import su.afk.yummy.tv.feature.player.common.PlayerBufferingIndicator
import su.afk.yummy.tv.feature.player.common.PlayerEndPromptCountdownEffect
import su.afk.yummy.tv.feature.player.common.PlayerEndPromptState
import su.afk.yummy.tv.feature.player.common.PlayerKeepScreenOnEffect
import su.afk.yummy.tv.feature.player.common.PlayerProgressSource
import su.afk.yummy.tv.feature.player.common.isVisible
import su.afk.yummy.tv.feature.player.common.playerEndPromptFor
import su.afk.yummy.tv.feature.player.common.rememberPlayerBufferingState
import su.afk.yummy.tv.feature.player.common.rememberPlayerCompletionTracker
import su.afk.yummy.tv.feature.player.common.rememberPlayerMediaReadyState
import su.afk.yummy.tv.feature.player.common.rememberPlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.common.rememberPlayerProgressReporter
import su.afk.yummy.tv.feature.player.common.rememberPlayerStepSeekToastState
import su.afk.yummy.tv.feature.player.common.service.PlayerMediaItemUpdater
import su.afk.yummy.tv.feature.player.common.service.rememberPlayerMediaController
import su.afk.yummy.tv.feature.player.common.service.rememberPlayerPlaybackConfig
import su.afk.yummy.tv.feature.player.common.toastIcon
import su.afk.yummy.tv.feature.player.model.MobilePlayerSettingsMode
import su.afk.yummy.tv.feature.player.model.MobilePlayerTrackSettingsTab
import su.afk.yummy.tv.feature.player.model.MobileVerticalGestureZone
import su.afk.yummy.tv.feature.player.model.MobileVideoTransform
import su.afk.yummy.tv.feature.player.model.rememberMobilePlayerGestureController
import su.afk.yummy.tv.feature.player.model.rememberMobilePlayerOverlayController
import su.afk.yummy.tv.feature.player.model.rememberMobilePlayerSeekController
import su.afk.yummy.tv.feature.player.pip.MobilePlayerPipController
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.utils.buildMobileMediaItemKey
import su.afk.yummy.tv.feature.player.utils.buildMobilePlayerMediaItemConfig
import su.afk.yummy.tv.feature.player.utils.buildMobilePlayerPlaybackKey
import su.afk.yummy.tv.feature.player.utils.gestureIcon
import su.afk.yummy.tv.feature.player.utils.mobilePlayerNotificationMeta
import su.afk.yummy.tv.feature.player.utils.toGesturePercentText

@OptIn(UnstableApi::class)
@Composable
internal fun MobileNativePlayer(
    state: PlayerState.State,
    streamUrl: String,
    videoTransform: MobileVideoTransform,
    onVideoTransformChanged: (MobileVideoTransform) -> Unit,
    onEvent: (PlayerState.Event) -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { MobilePlayerPipController.findActivity(context) }
    val supportsPictureInPicture = remember(context) { MobilePlayerPipController.canEnter(context) }
    val isInPictureInPictureMode = MobilePlayerPipController.isInPictureInPictureMode
    val pipSession = remember { MobilePlayerPipController.createSession() }
    val playerNamePrefix = stringResource(R.string.player_name_prefix)
    val ui = rememberPlayerPlaybackUiState(state, playerNamePrefix)
    val qualities = remember(streamUrl, state.streamQualityMap) {
        state.streamQualityMap ?: deriveQualityUrls(streamUrl)
    }
    val selectedQuality = state.selectedQuality?.takeIf { it in qualities }
        ?: qualities.keys.lastOrNull()
    val selectedSpeed = state.selectedSpeed
    val currentPosition = state.playbackPositionMs.takeIf { it > 0L } ?: state.resumeFromMs
    val duration = state.playbackDurationMs
    var settingsMode by remember { mutableStateOf<MobilePlayerSettingsMode?>(null) }
    var settingsTrackTab by remember { mutableStateOf(MobilePlayerTrackSettingsTab.Dubbing) }
    var wantsPlay by remember { mutableStateOf(true) }
    val resumeAfterLifecyclePause = remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var nextEpisodePromptState by remember(ui.activeIframeUrl, streamUrl) {
        mutableStateOf<PlayerEndPromptState>(PlayerEndPromptState.Hidden)
    }
    var seekProgress by remember { mutableFloatStateOf(0f) }
    var bufferedProgress by remember(streamUrl, ui.activeIframeUrl) { mutableFloatStateOf(0f) }
    val skippedSegments = remember(streamUrl) { mutableStateListOf<String>() }
    val currentUrl = selectedQuality?.let(qualities::get) ?: streamUrl
    val playbackConfigKey = remember(currentUrl, state.streamHeaders, state.retryKey) {
        buildMobilePlayerPlaybackKey(state = state, url = currentUrl)
    }
    val mediaController = rememberPlayerMediaController()
    val playbackConfig = rememberPlayerPlaybackConfig()
    val stepSeekToast = rememberPlayerStepSeekToastState(
        streamUrl = streamUrl,
        toastDuration = MOBILE_PLAYER_SEEK_TOAST_DURATION,
    )
    val overlay = rememberMobilePlayerOverlayController(
        canHide = { wantsPlay && settingsMode == null && !isSeeking },
        wantsPlay = { wantsPlay },
        isPromptVisible = { nextEpisodePromptState.isVisible },
    )
    val gestures = rememberMobilePlayerGestureController(
        activity = activity,
        initialTransform = videoTransform,
        onGestureStart = { overlay.cancelHide() },
        onVideoTransformChanged = onVideoTransformChanged,
    )
    val effectiveSpeed = if (gestures.isSpeedBoosted) MOBILE_PLAYER_SPEED_BOOST else selectedSpeed
    val mediaItemUpdater = remember { PlayerMediaItemUpdater() }
    val transformScopeKey = remember(state.animeId, state.animeTitle, ui.activeBalancerName) {
        "${state.animeId}|${state.animeTitle}|${ui.activeBalancerName}"
    }
    val notificationMeta = mobilePlayerNotificationMeta(ui)
    var notificationArtworkUrl by remember { mutableStateOf<String?>(null) }
    val mediaItemKey = remember(
        playbackConfigKey,
        state.animeTitle,
        notificationMeta,
        notificationArtworkUrl,
        state.playbackDurationMs,
    ) {
        buildMobileMediaItemKey(
            playbackKey = playbackConfigKey,
            animeTitle = state.animeTitle,
            meta = notificationMeta,
            artworkUrl = notificationArtworkUrl,
            durationMs = state.playbackDurationMs,
        )
    }

    LaunchedEffect(ui.activeScreenshotUrl, ui.activeIframeUrl, state.posterUrl) {
        notificationArtworkUrl = state.posterUrl.takeIf { it.isNotBlank() }
        notificationArtworkUrl = resolveContinueWatchingImage(
            screenshotUrl = ui.activeScreenshotUrl,
            episodeUrl = ui.activeIframeUrl,
            posterUrl = state.posterUrl,
        )
    }

    DisposableEffect(pipSession) {
        MobilePlayerPipController.registerSession(pipSession)
        onDispose {
            MobilePlayerPipController.unregisterSession(pipSession)
        }
    }

    PlayerKeepScreenOnEffect()

    val player = mediaController
    val isBuffering = rememberPlayerBufferingState(player)
    val isMediaReady = rememberPlayerMediaReadyState(player, playbackConfigKey)

    LaunchedEffect(player, playbackConfigKey, mediaItemKey, ui.activeIframeUrl) {
        val activePlayer = player ?: return@LaunchedEffect
        mediaItemUpdater.update(
            player = activePlayer,
            playbackConfig = playbackConfig,
            config = buildMobilePlayerMediaItemConfig(
                playbackKey = playbackConfigKey,
                mediaItemKey = mediaItemKey,
                url = currentUrl,
                episodeUrl = ui.activeIframeUrl,
                state = state,
                meta = notificationMeta,
                artworkUrl = notificationArtworkUrl,
            ),
        )
        activePlayer.playWhenReady = wantsPlay
    }

    if (player == null) {
        PlayerBlackBackdrop()
        return
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

    fun handleEpisodeEnd(positionMs: Long, durationMs: Long) {
        completionTracker.onEpisodeEnd(positionMs, durationMs)
        if (
            (ui.hasNextEpisode || ui.nextEpisodeDubbing != null) &&
            !isInPictureInPictureMode &&
            !nextEpisodePromptState.isVisible
        ) {
            // Авто-отсчёт только внутри текущей озвучки: смену озвучки
            // пользователь должен подтвердить явно
            nextEpisodePromptState =
                playerEndPromptFor(state.autoPlayNextEpisode && ui.hasNextEpisode)
            overlay.visible = false
            settingsMode = null
            overlay.cancelHide()
        }
    }

    val seekController = rememberMobilePlayerSeekController(
        player = player,
        fallbackDurationMs = { duration },
        reporter = reporter,
        stepSeekToast = stepSeekToast,
        onEpisodeEnd = ::handleEpisodeEnd,
        onBackwardStep = { nextEpisodePromptState = PlayerEndPromptState.Hidden },
    )

    MobilePlayerListenerEffect(
        player = player,
        activity = activity,
        pipSession = pipSession,
        reporter = reporter,
        overlay = overlay,
        stepSeekToast = stepSeekToast,
        seekController = seekController,
        fallbackDurationMs = { duration },
        wantsPlay = { wantsPlay },
        onWantsPlayChanged = { wantsPlay = it },
        onEpisodeEnd = ::handleEpisodeEnd,
        onEvent = onEvent,
    )

    MobilePlayerLifecycleEffect(
        player = player,
        pipSession = pipSession,
        reporter = reporter,
        resumeAfterPause = resumeAfterLifecyclePause,
        fallbackDurationMs = { duration },
        wantsPlay = { wantsPlay },
        promptState = { nextEpisodePromptState },
        onPromptStateChange = { nextEpisodePromptState = it },
    )

    LaunchedEffect(isInPictureInPictureMode) {
        if (isInPictureInPictureMode) {
            overlay.visible = false
            nextEpisodePromptState = PlayerEndPromptState.Hidden
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
        nextEpisodePromptState = PlayerEndPromptState.Hidden
        settingsMode = null
    }

    PlayerEndPromptCountdownEffect(
        promptState = nextEpisodePromptState,
        contentKey = ui.activeIframeUrl,
        onPromptStateChange = { nextEpisodePromptState = it },
        onFinished = {
            nextEpisodePromptState = PlayerEndPromptState.Hidden
            onEvent(PlayerState.Event.NextEpisode(PlayerNextEpisodeSource.EndPrompt))
        },
    )

    BackHandler(enabled = nextEpisodePromptState.isVisible && !isInPictureInPictureMode) {
        nextEpisodePromptState = PlayerEndPromptState.Hidden
        overlay.show()
    }

    LaunchedEffect(videoTransform) {
        if (!gestures.transformGestureActive) {
            gestures.liveVideoTransform = videoTransform
        }
    }

    LaunchedEffect(player, effectiveSpeed, wantsPlay) {
        player.setPlaybackSpeed(effectiveSpeed)
        player.playWhenReady = wantsPlay
        pipSession.setPlaying(wantsPlay, activity)
    }

    MobilePlayerProgressPollingEffect(
        player = player,
        episodeKey = ui.activeIframeUrl,
        isMediaReady = isMediaReady,
        autoSkipOpeningsEndings = state.autoSkipOpeningsEndings,
        reporter = reporter,
        skippedSegments = skippedSegments,
        isSeeking = { isSeeking },
        currentPositionMs = { currentPosition },
        fallbackDurationMs = { duration },
        activeSkips = { ui.activeSkips },
        onBufferedProgressChange = { bufferedProgress = it },
    )

    val displayTime = if (isSeeking && duration > 0) {
        (seekProgress * duration).toLong()
    } else {
        currentPosition
    }
    val progress = when {
        isSeeking -> seekProgress
        duration > 0 -> currentPosition.toFloat() / duration
        else -> 0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { gestures.playerSize = it },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
        ) {
            ContentFrame(
                player = player,
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                contentScale = ContentScale.Fit,
                keepContentOnReset = state.isAllohaPlaybackRecovering,
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
                            .background(Color.Black)
                    )
                },
            )
        }

        PlayerBufferingIndicator(
            visible = isBuffering || state.isAllohaPlaybackRecovering,
            modifier = Modifier.align(Alignment.Center),
        )

        MobilePlayerGestureLayer(
            enabled = !isInPictureInPictureMode,
            onTap = { overlay.toggle() },
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
            showDetails = state.animeId > 0,
            showPictureInPicture = supportsPictureInPicture && !isInPictureInPictureMode,
            visible = overlay.visible && !isInPictureInPictureMode,
        )

        MobilePlayerOverlay(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = overlay.visible && !isInPictureInPictureMode,
            wantsPlay = wantsPlay,
            displayTime = displayTime,
            duration = duration,
            seekProgress = progress,
            bufferedProgress = bufferedProgress,
            hasPrevEpisode = ui.hasPrevEpisode,
            hasNextEpisode = ui.hasNextEpisode,
            onPlayPause = {
                if (wantsPlay) player.pause() else player.play()
                overlay.show()
            },
            onSeekChange = { value ->
                isSeeking = true
                seekProgress = value
                overlay.visible = true
                overlay.cancelHide()
            },
            onSeekFinished = {
                if (duration > 0) {
                    val newPosition = (seekProgress * duration).toLong().coerceIn(0L, duration)
                    seekController.seekTo(newPosition)
                }
                isSeeking = false
                overlay.show()
            },
            onPrevEpisode = { onEvent(PlayerState.Event.PrevEpisode) },
            onNextEpisode = {
                nextEpisodePromptState = PlayerEndPromptState.Hidden
                onEvent(PlayerState.Event.NextEpisode(PlayerNextEpisodeSource.Controls))
            },
            onTrackSettings = {
                nextEpisodePromptState = PlayerEndPromptState.Hidden
                settingsTrackTab = MobilePlayerTrackSettingsTab.Dubbing
                settingsMode = MobilePlayerSettingsMode.Track
                overlay.visible = true
                overlay.cancelHide()
            },
            onPlaybackSettings = {
                nextEpisodePromptState = PlayerEndPromptState.Hidden
                settingsMode = MobilePlayerSettingsMode.Playback
                overlay.visible = true
                overlay.cancelHide()
            },
        )

        MobilePlayerSeekToast(
            text = stepSeekToast.text.takeUnless { isInPictureInPictureMode },
            icon = stepSeekToast.direction.toastIcon,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (overlay.visible && !isInPictureInPictureMode) 128.dp else 36.dp),
        )

        MobilePlayerZoomIndicator(
            visible = gestures.transformGestureActive && !isInPictureInPictureMode,
            scale = gestures.liveVideoTransform.scale,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp),
        )

        MobilePlayerGestureIndicator(
            visible = gestures.brightnessGestureActive && !isInPictureInPictureMode,
            icon = MobileVerticalGestureZone.Brightness.gestureIcon,
            percentText = gestures.brightnessLevel.toGesturePercentText(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 32.dp),
        )

        MobilePlayerGestureIndicator(
            visible = gestures.volumeGestureActive && !isInPictureInPictureMode,
            icon = MobileVerticalGestureZone.Volume.gestureIcon,
            percentText = gestures.volumeLevel.toGesturePercentText(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp),
        )

        MobilePlayerSpeedBoostIndicator(
            visible = gestures.isSpeedBoosted && !isInPictureInPictureMode,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp),
        )

        val canChangePlayer = ui.balancerNames.size > 1
        val canChangeDubbing = ui.dubbingNames.size > 1
        if (state.isAllohaPlaybackRecovering && state.showChangePlayerHint &&
            (canChangePlayer || canChangeDubbing) && !isInPictureInPictureMode
        ) {
            MobilePlayerRecoveryHint(
                onChangePlayer = if (canChangePlayer) {
                    {
                        settingsTrackTab = MobilePlayerTrackSettingsTab.Player
                        settingsMode = MobilePlayerSettingsMode.Track
                        overlay.cancelHide()
                    }
                } else {
                    null
                },
                onChangeDubbing = if (canChangeDubbing) {
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

        if (nextEpisodePromptState.isVisible &&
            (ui.hasNextEpisode || ui.nextEpisodeDubbing != null) &&
            !isInPictureInPictureMode
        ) {
            MobilePlayerEndPrompt(
                title = when (val prompt = nextEpisodePromptState) {
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
                onPrimary = {
                    nextEpisodePromptState = PlayerEndPromptState.Hidden
                    onEvent(PlayerState.Event.NextEpisode(PlayerNextEpisodeSource.EndPrompt))
                },
                onStay = {
                    nextEpisodePromptState = PlayerEndPromptState.Hidden
                    overlay.show()
                },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        val activeSettingsMode = settingsMode
        if (activeSettingsMode != null && !isInPictureInPictureMode) {
            MobilePlayerSettingsSheet(
                mode = activeSettingsMode,
                qualities = qualities.keys.toList(),
                selectedQuality = selectedQuality,
                onQualitySelected = { quality ->
                    val position = player.currentPosition.coerceAtLeast(0)
                    reporter.saveProgress(position, duration)
                    onEvent(PlayerState.Event.QualitySelected(quality, position))
                },
                speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f),
                selectedSpeed = selectedSpeed,
                onSpeedSelected = { onEvent(PlayerState.Event.SpeedSelected(it)) },
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
                            player.currentPosition
                        )
                    )
                },
                balancerNames = ui.balancerNames,
                balancerAvailability = ui.balancerAvailability,
                selectedBalancerIndex = ui.currentBalancerIndex,
                onBalancerSelected = { index ->
                    val balancerIndex =
                        ui.availableBalancerIndices.getOrElse(index) { state.sourceSelection.balancerIndex }
                    onEvent(
                        PlayerState.Event.BalancerSelected(
                            balancerIndex,
                            player.currentPosition
                        )
                    )
                },
                onDismiss = { settingsMode = null },
                initialTrackTab = settingsTrackTab,
            )
        }
    }
}

private const val MOBILE_PLAYER_SPEED_BOOST = 2f
