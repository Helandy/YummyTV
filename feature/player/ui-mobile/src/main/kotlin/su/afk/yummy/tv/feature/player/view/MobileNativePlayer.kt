package su.afk.yummy.tv.feature.player.view

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.feature.player.PlayerProgressSnapshot
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.model.MobilePlayerSettingsMode
import su.afk.yummy.tv.feature.player.model.MobilePlayerUiState
import su.afk.yummy.tv.feature.player.model.MobileSeekDirection
import su.afk.yummy.tv.feature.player.model.MobileVideoTransform
import su.afk.yummy.tv.feature.player.pip.MobilePlayerPipController
import su.afk.yummy.tv.feature.player.utils.MOBILE_PLAYER_STEP_SEEK_OFFSETS_MS
import su.afk.yummy.tv.feature.player.utils.MOBILE_PLAYER_STEP_SEEK_RESET_MS
import su.afk.yummy.tv.feature.player.utils.applyMobileVideoTransform
import su.afk.yummy.tv.feature.player.utils.calculateMobileVideoTransform
import su.afk.yummy.tv.feature.player.utils.formatSignedSeconds
import su.afk.yummy.tv.feature.player.utils.mediaItemFor
import su.afk.yummy.tv.feature.player.utils.segments
import su.afk.yummy.tv.feature.player.utils.toastIcon

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
    val ui = remember(state) { MobilePlayerUiState.from(state) }
    val qualities = remember(streamUrl, state.streamQualityMap) {
        state.streamQualityMap?.takeIf { it.isNotEmpty() } ?: deriveQualityUrls(streamUrl)
    }
    val selectedQuality = state.selectedQuality?.takeIf { it in qualities } ?: qualities.keys.last()
    val selectedSpeed = state.selectedSpeed
    val currentPosition = state.playbackPositionMs.takeIf { it > 0L } ?: state.resumeFromMs
    val duration = state.playbackDurationMs
    var lastSaveTime by remember { mutableStateOf(0L) }
    var settingsMode by remember { mutableStateOf<MobilePlayerSettingsMode?>(null) }
    var overlayVisible by remember { mutableStateOf(true) }
    var wantsPlay by remember { mutableStateOf(true) }
    var resumeAfterLifecyclePause by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }
    var playerSize by remember { mutableStateOf(IntSize.Zero) }
    var lastStepSeekTime by remember(streamUrl) { mutableLongStateOf(0L) }
    var stepSeekCount by remember(streamUrl) { mutableStateOf(0) }
    var stepSeekTotalOffset by remember(streamUrl) { mutableLongStateOf(0L) }
    var lastStepSeekDirection by remember(streamUrl) { mutableStateOf<MobileSeekDirection?>(null) }
    var stepSeekToastText by remember(streamUrl) { mutableStateOf<String?>(null) }
    var stepSeekToastIcon by remember { mutableStateOf(MobileSeekDirection.Forward.toastIcon) }
    val skippedSegments = remember(streamUrl) { mutableStateListOf<String>() }
    val currentUrl = qualities[selectedQuality] ?: streamUrl
    val coroutineScope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }
    var stepSeekToastJob by remember { mutableStateOf<Job?>(null) }
    val playerViewHolder = remember { arrayOfNulls<PlayerView>(1) }
    var liveVideoTransform by remember { mutableStateOf(videoTransform) }
    var transformGestureActive by remember { mutableStateOf(false) }
    val transformScopeKey = remember(state.animeId, state.animeTitle, ui.activeBalancerName) {
        "${state.animeId}|${state.animeTitle}|${ui.activeBalancerName}"
    }

    DisposableEffect(Unit) {
        MobilePlayerPipController.setPlayerActive(true)
        onDispose {
            MobilePlayerPipController.setPlayerActive(false)
            playerViewHolder[0] = null
        }
    }

    fun scheduleOverlayHide() {
        hideJob?.cancel()
        hideJob = coroutineScope.launch {
            delay(4_000)
            if (wantsPlay && settingsMode == null && !isSeeking) {
                overlayVisible = false
            }
        }
    }

    fun showOverlay() {
        overlayVisible = true
        if (wantsPlay) scheduleOverlayHide() else hideJob?.cancel()
    }

    fun toggleOverlay() {
        if (overlayVisible) {
            hideJob?.cancel()
            overlayVisible = false
        } else {
            showOverlay()
        }
    }

    fun applyVideoTransform(centroid: Offset, pan: Offset, zoomChange: Float) {
        val transform = calculateMobileVideoTransform(
            currentScale = liveVideoTransform.scale,
            currentOffset = liveVideoTransform.offset,
            playerSize = playerSize,
            centroid = centroid,
            pan = pan,
            zoomChange = zoomChange,
        )
        liveVideoTransform = transform
        playerViewHolder[0]?.applyMobileVideoTransform(transform.scale, transform.offset)
        onVideoTransformChanged(transform)
    }

    val exoPlayer = remember(currentUrl, state.streamHeaders) {
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setForceHighestSupportedBitrate(true))
        }
        val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
            state.streamHeaders["User-Agent"]?.takeIf { it.isNotBlank() }?.let(::setUserAgent)
            val requestHeaders = state.streamHeaders.filterKeys { !it.equals("User-Agent", ignoreCase = true) }
            if (requestHeaders.isNotEmpty()) setDefaultRequestProperties(requestHeaders)
        }
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                setMediaItem(mediaItemFor(currentUrl))
                seekTo(state.resumeFromMs)
                playWhenReady = wantsPlay
                prepare()
            }
    }

    fun saveProgress(positionMs: Long = currentPosition, durationMs: Long = duration) {
        if (durationMs <= 0 || ui.activeIframeUrl.isBlank()) return
        onEvent(
            PlayerState.Event.SaveProgress(
                PlayerProgressSnapshot(
                    episode = ui.activeEpisode,
                    episodeUrl = ui.activeIframeUrl,
                    videoId = ui.activeVideoId,
                    playerName = ui.activeBalancerName,
                    dubbing = ui.activeDubbing,
                    screenshotUrl = ui.activeScreenshotUrl,
                    positionMs = positionMs,
                    durationMs = durationMs,
                )
            )
        )
        lastSaveTime = System.currentTimeMillis()
    }

    fun seekToPosition(positionMs: Long) {
        val playerDuration = exoPlayer.duration.takeIf { it > 0 } ?: duration
        val clamped = if (playerDuration > 0) {
            positionMs.coerceIn(0L, playerDuration)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        exoPlayer.seekTo(clamped)
        onEvent(
            PlayerState.Event.PlaybackPositionChanged(
                clamped,
                playerDuration.coerceAtLeast(0L)
            )
        )
        saveProgress(clamped, playerDuration)
    }

    fun stepSeek(direction: MobileSeekDirection) {
        val now = System.currentTimeMillis()
        if (
            now - lastStepSeekTime > MOBILE_PLAYER_STEP_SEEK_RESET_MS ||
            lastStepSeekDirection != direction
        ) {
            stepSeekCount = 0
            stepSeekTotalOffset = 0L
        }

        stepSeekCount = (stepSeekCount + 1).coerceAtMost(MOBILE_PLAYER_STEP_SEEK_OFFSETS_MS.size)
        lastStepSeekTime = now
        lastStepSeekDirection = direction

        val offset = MOBILE_PLAYER_STEP_SEEK_OFFSETS_MS[stepSeekCount - 1] * direction.sign
        stepSeekTotalOffset += offset
        seekToPosition(exoPlayer.currentPosition + offset)
        stepSeekToastText = stepSeekTotalOffset.formatSignedSeconds()
        stepSeekToastIcon = direction.toastIcon
        stepSeekToastJob?.cancel()
        stepSeekToastJob = coroutineScope.launch {
            delay(MOBILE_PLAYER_SEEK_TOAST_DURATION_MS)
            stepSeekToastText = null
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                wantsPlay = playWhenReady
                MobilePlayerPipController.setPlaying(playWhenReady, activity)
                if (playWhenReady) scheduleOverlayHide() else hideJob?.cancel()
            }

            override fun onPlayerError(error: PlaybackException) {
                onEvent(PlayerState.Event.PlaybackError(error.localizedMessage ?: error.errorCodeName))
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                MobilePlayerPipController.setAspectRatio(videoSize.width, videoSize.height)
            }
        }
        exoPlayer.addListener(listener)
        MobilePlayerPipController.setPlaying(exoPlayer.playWhenReady, activity)
        MobilePlayerPipController.setPlayPauseAction {
            if (exoPlayer.playWhenReady) exoPlayer.pause() else exoPlayer.play()
        }
        if (wantsPlay) scheduleOverlayHide()
        onDispose {
            hideJob?.cancel()
            stepSeekToastJob?.cancel()
            MobilePlayerPipController.setPlaying(false, activity)
            MobilePlayerPipController.setPlayPauseAction(null)
            val position = exoPlayer.currentPosition.coerceAtLeast(0)
            val dur = exoPlayer.duration.takeIf { it > 0 } ?: duration
            onEvent(PlayerState.Event.PlaybackPositionChanged(position, dur))
            saveProgress(position, dur)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val keepPlayingInPip = MobilePlayerPipController.shouldKeepPlayingOnPause()
                    resumeAfterLifecyclePause = wantsPlay && !keepPlayingInPip
                    val position = exoPlayer.currentPosition.coerceAtLeast(0)
                    val dur = exoPlayer.duration.takeIf { it > 0 } ?: duration
                    onEvent(PlayerState.Event.PlaybackPositionChanged(position, dur))
                    saveProgress(position, dur)
                    if (!keepPlayingInPip) {
                        exoPlayer.pause()
                    }
                }

                Lifecycle.Event.ON_RESUME -> if (resumeAfterLifecyclePause) exoPlayer.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isInPictureInPictureMode) {
        if (isInPictureInPictureMode) {
            overlayVisible = false
            transformGestureActive = false
            settingsMode = null
            stepSeekToastText = null
            hideJob?.cancel()
            stepSeekToastJob?.cancel()
        }
    }

    LaunchedEffect(transformScopeKey) {
        transformGestureActive = false
        liveVideoTransform = videoTransform
    }

    LaunchedEffect(videoTransform) {
        if (!transformGestureActive) {
            liveVideoTransform = videoTransform
        }
    }

    LaunchedEffect(exoPlayer, selectedSpeed, wantsPlay) {
        exoPlayer.setPlaybackSpeed(selectedSpeed)
        exoPlayer.playWhenReady = wantsPlay
        MobilePlayerPipController.setPlaying(wantsPlay, activity)
    }

    LaunchedEffect(exoPlayer, ui.activeIframeUrl, state.autoSkipOpeningsEndings) {
        while (true) {
            var position = currentPosition
            var dur = duration
            if (!isSeeking) {
                position = exoPlayer.currentPosition.coerceAtLeast(0)
                dur = exoPlayer.duration.takeIf { it > 0 } ?: 0L
                onEvent(PlayerState.Event.PlaybackPositionChanged(position, dur))
            }
            val now = System.currentTimeMillis()
            if (dur > 0 && now - lastSaveTime >= 10_000L) {
                saveProgress(position, dur)
            }
            if (state.autoSkipOpeningsEndings) {
                ui.activeSkips.segments().forEach { (key, segment) ->
                    val segmentKey = "${ui.activeIframeUrl}-$key"
                    if (segmentKey !in skippedSegments &&
                        position in segment.startMs..segment.endMs
                    ) {
                        skippedSegments += segmentKey
                        exoPlayer.seekTo(segment.endMs)
                    }
                }
            }
            delay(1_000)
        }
    }

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
            .onSizeChanged { playerSize = it },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    playerViewHolder[0] = this
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    clipChildren = true
                    clipToPadding = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    applyMobileVideoTransform(liveVideoTransform.scale, liveVideoTransform.offset)
                }
            },
            update = {
                playerViewHolder[0] = it
                it.player = exoPlayer
                it.applyMobileVideoTransform(liveVideoTransform.scale, liveVideoTransform.offset)
            },
        )

        MobilePlayerGestureLayer(
            enabled = !isInPictureInPictureMode,
            onTap = { toggleOverlay() },
            onDoubleTap = ::stepSeek,
            onTransformStart = {
                transformGestureActive = true
                hideJob?.cancel()
            },
            onTransform = ::applyVideoTransform,
            onTransformEnd = {
                transformGestureActive = false
            },
        )

        MobilePlayerTopBar(
            title = state.animeTitle,
            episode = ui.activeEpisode,
            dubbing = ui.activeDubbing,
            playerName = ui.activeBalancerName,
            onBack = { onEvent(PlayerState.Event.Back) },
            onPictureInPicture = { activity?.let(MobilePlayerPipController::enter) },
            showPictureInPicture = supportsPictureInPicture && !isInPictureInPictureMode,
            visible = overlayVisible && !isInPictureInPictureMode,
        )

        MobilePlayerOverlay(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = overlayVisible && !isInPictureInPictureMode,
            wantsPlay = wantsPlay,
            displayTime = displayTime,
            duration = duration,
            seekProgress = progress,
            hasPrevEpisode = ui.hasPrevEpisode,
            hasNextEpisode = ui.hasNextEpisode,
            onPlayPause = {
                if (wantsPlay) exoPlayer.pause() else exoPlayer.play()
                showOverlay()
            },
            onSeekChange = { value ->
                isSeeking = true
                seekProgress = value
                overlayVisible = true
                hideJob?.cancel()
            },
            onSeekFinished = {
                if (duration > 0) {
                    val newPosition = (seekProgress * duration).toLong().coerceIn(0L, duration)
                    exoPlayer.seekTo(newPosition)
                    onEvent(PlayerState.Event.PlaybackPositionChanged(newPosition, duration))
                    saveProgress(newPosition)
                }
                isSeeking = false
                showOverlay()
            },
            onPrevEpisode = { onEvent(PlayerState.Event.PrevEpisode) },
            onNextEpisode = { onEvent(PlayerState.Event.NextEpisode) },
            onTrackSettings = {
                settingsMode = MobilePlayerSettingsMode.Track
                overlayVisible = true
                hideJob?.cancel()
            },
            onPlaybackSettings = {
                settingsMode = MobilePlayerSettingsMode.Playback
                overlayVisible = true
                hideJob?.cancel()
            },
        )

        MobilePlayerSeekToast(
            text = stepSeekToastText.takeUnless { isInPictureInPictureMode },
            icon = stepSeekToastIcon,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (overlayVisible && !isInPictureInPictureMode) 128.dp else 36.dp),
        )

        MobilePlayerZoomIndicator(
            visible = transformGestureActive && !isInPictureInPictureMode,
            scale = liveVideoTransform.scale,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp),
        )

        val activeSettingsMode = settingsMode
        if (activeSettingsMode != null && !isInPictureInPictureMode) {
            MobilePlayerSettingsSheet(
                mode = activeSettingsMode,
                qualities = qualities.keys.toList(),
                selectedQuality = selectedQuality,
                onQualitySelected = { quality ->
                    val position = exoPlayer.currentPosition.coerceAtLeast(0)
                    saveProgress(position)
                    onEvent(PlayerState.Event.QualitySelected(quality, position))
                },
                speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f),
                selectedSpeed = selectedSpeed,
                onSpeedSelected = { onEvent(PlayerState.Event.SpeedSelected(it)) },
                dubbingNames = ui.dubbingNames,
                selectedDubbingIndex = ui.currentDubbingIndex,
                onDubbingSelected = { onEvent(PlayerState.Event.DubbingSelected(it, exoPlayer.currentPosition)) },
                balancerNames = ui.balancerNames,
                selectedBalancerIndex = ui.currentBalancerIndex,
                onBalancerSelected = { onEvent(PlayerState.Event.BalancerSelected(it, exoPlayer.currentPosition)) },
                onDismiss = { settingsMode = null },
            )
        }
    }
}
