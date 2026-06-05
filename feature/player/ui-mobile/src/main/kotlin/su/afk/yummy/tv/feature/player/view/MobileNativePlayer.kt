package su.afk.yummy.tv.feature.player.view

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import su.afk.yummy.tv.feature.player.pip.MobilePlayerPipController
import su.afk.yummy.tv.feature.player.utils.mediaItemFor
import su.afk.yummy.tv.feature.player.utils.segments

@OptIn(UnstableApi::class)
@Composable
internal fun MobileNativePlayer(
    state: PlayerState.State,
    streamUrl: String,
    onEvent: (PlayerState.Event) -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { MobilePlayerPipController.findActivity(context) }
    val supportsPictureInPicture = remember(context) { MobilePlayerPipController.canEnter(context) }
    val isInPictureInPictureMode = MobilePlayerPipController.isInPictureInPictureMode
    val ui = remember(state) { MobilePlayerUiState.from(state) }
    val qualities = remember(streamUrl, state.streamQualityMap) {
        state.streamQualityMap?.takeIf { it.isNotEmpty() } ?: linkedMapOf("auto" to streamUrl)
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
    val skippedSegments = remember(streamUrl) { mutableStateListOf<String>() }
    val currentUrl = qualities[selectedQuality] ?: streamUrl
    val coroutineScope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        MobilePlayerPipController.setPlayerActive(true)
        onDispose { MobilePlayerPipController.setPlayerActive(false) }
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
            settingsMode = null
            hideJob?.cancel()
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
            .background(Color.Black),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            update = { it.player = exoPlayer },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    enabled = !isInPictureInPictureMode,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { toggleOverlay() },
                ),
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
