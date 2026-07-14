package su.afk.yummy.tv.feature.player.view

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.utils.resolveContinueWatchingImage
import su.afk.yummy.tv.feature.player.PlayerNextEpisodeSource
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.common.PLAYER_END_PROMPT_COUNTDOWN_SECONDS
import su.afk.yummy.tv.feature.player.common.PlayerEndPromptState
import su.afk.yummy.tv.feature.player.common.StepSeekAccumulator
import su.afk.yummy.tv.feature.player.common.analyticsType
import su.afk.yummy.tv.feature.player.common.calculateBufferedProgress
import su.afk.yummy.tv.feature.player.common.diagnosticCauseChain
import su.afk.yummy.tv.feature.player.common.formatSignedSeconds
import su.afk.yummy.tv.feature.player.common.isVisible
import su.afk.yummy.tv.feature.player.common.rememberPlayerBufferingState
import su.afk.yummy.tv.feature.player.common.service.PlayerAudioTrackPolicy
import su.afk.yummy.tv.feature.player.common.service.PlayerMediaItemConfig
import su.afk.yummy.tv.feature.player.common.service.PlayerMediaItemUpdater
import su.afk.yummy.tv.feature.player.common.service.rememberPlayerMediaController
import su.afk.yummy.tv.feature.player.common.service.rememberPlayerPlaybackConfig
import su.afk.yummy.tv.feature.player.isAllohaPlayerUrl
import su.afk.yummy.tv.feature.player.model.MobilePlayerSettingsMode
import su.afk.yummy.tv.feature.player.model.MobilePlayerUiState
import su.afk.yummy.tv.feature.player.model.MobileSeekDirection
import su.afk.yummy.tv.feature.player.model.MobileVideoTransform
import su.afk.yummy.tv.feature.player.pip.MobilePlayerPipCallbacks
import su.afk.yummy.tv.feature.player.pip.MobilePlayerPipController
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.utils.MOBILE_PLAYER_PIP_SEEK_STEP_MS
import su.afk.yummy.tv.feature.player.utils.buildProgressSnapshot
import su.afk.yummy.tv.feature.player.utils.calculateMobileVideoTransform
import su.afk.yummy.tv.feature.player.utils.isAtMobilePlayerEnd
import su.afk.yummy.tv.feature.player.utils.segments
import su.afk.yummy.tv.feature.player.utils.toStepSeekDirection
import su.afk.yummy.tv.feature.player.utils.toastIcon
import kotlin.time.Duration.Companion.seconds

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
    val hostView = LocalView.current
    val activity = remember(context) { MobilePlayerPipController.findActivity(context) }
    val supportsPictureInPicture = remember(context) { MobilePlayerPipController.canEnter(context) }
    val isInPictureInPictureMode = MobilePlayerPipController.isInPictureInPictureMode
    val pipSession = remember { MobilePlayerPipController.createSession() }
    val playerNamePrefix = stringResource(R.string.player_name_prefix)
    val ui = remember(state, playerNamePrefix) {
        MobilePlayerUiState.from(
            state = state,
            playerNamePrefix = playerNamePrefix,
        )
    }
    val qualities = remember(streamUrl, state.streamQualityMap) {
        state.streamQualityMap ?: deriveQualityUrls(streamUrl)
    }
    val selectedQuality = state.selectedQuality?.takeIf { it in qualities }
        ?: qualities.keys.lastOrNull()
    val selectedSpeed = state.selectedSpeed
    val currentPosition = state.playbackPositionMs.takeIf { it > 0L } ?: state.resumeFromMs
    val duration = state.playbackDurationMs
    var lastSaveTime by remember { mutableStateOf(0L) }
    var settingsMode by remember { mutableStateOf<MobilePlayerSettingsMode?>(null) }
    var overlayVisible by remember { mutableStateOf(true) }
    var wantsPlay by remember { mutableStateOf(true) }
    var resumeAfterLifecyclePause by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var nextEpisodePromptState by remember(ui.activeIframeUrl, streamUrl) {
        mutableStateOf<PlayerEndPromptState>(PlayerEndPromptState.Hidden)
    }
    var completionReported by remember(ui.activeIframeUrl, streamUrl) { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }
    var bufferedProgress by remember(streamUrl, ui.activeIframeUrl) { mutableFloatStateOf(0f) }
    var playerSize by remember { mutableStateOf(IntSize.Zero) }
    var stepSeekToastText by remember(streamUrl) { mutableStateOf<String?>(null) }
    var stepSeekToastIcon by remember { mutableStateOf(MobileSeekDirection.Forward.toastIcon) }
    val skippedSegments = remember(streamUrl) { mutableStateListOf<String>() }
    val stepSeekAccumulator = remember(streamUrl) { StepSeekAccumulator() }
    val currentUrl = selectedQuality?.let(qualities::get) ?: streamUrl
    val playbackConfigKey = remember(currentUrl, state.streamHeaders, state.retryKey) {
        buildString {
            append(currentUrl).append('|').append(state.retryKey)
            state.streamHeaders.entries
                .sortedBy { it.key.lowercase() }
                .forEach { (key, value) ->
                    append('|').append(key).append('=').append(value)
                }
        }
    }
    val mediaController = rememberPlayerMediaController()
    val playbackConfig = rememberPlayerPlaybackConfig()
    val coroutineScope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }
    var stepSeekToastJob by remember { mutableStateOf<Job?>(null) }
    var liveVideoTransform by remember { mutableStateOf(videoTransform) }
    var transformGestureActive by remember { mutableStateOf(false) }
    val mediaItemUpdater = remember { PlayerMediaItemUpdater() }
    val transformScopeKey = remember(state.animeId, state.animeTitle, ui.activeBalancerName) {
        "${state.animeId}|${state.animeTitle}|${ui.activeBalancerName}"
    }
    val notificationSubtitle = ui.activeEpisode.takeIf { it.isNotBlank() }?.let {
        stringResource(R.string.player_notification_episode, it)
    }
    val notificationDescription = when {
        ui.activeBalancerName.isNotBlank() && ui.activeDubbing.isNotBlank() ->
            stringResource(
                R.string.player_notification_details,
                ui.activeDubbing,
                ui.activeBalancerName,
            )

        else -> ui.activeBalancerName.ifBlank { ui.activeDubbing }.takeIf { it.isNotBlank() }
    }
    val notificationContentText = listOfNotNull(
        notificationSubtitle,
        ui.activeDubbing.takeIf { it.isNotBlank() },
        ui.activeBalancerName.takeIf { it.isNotBlank() },
    ).joinToString(separator = " • ")
    var notificationArtworkUrl by remember { mutableStateOf<String?>(null) }
    val mediaItemKey = remember(
        playbackConfigKey,
        state.animeTitle,
        notificationSubtitle,
        notificationDescription,
        notificationContentText,
        notificationArtworkUrl,
        state.playbackDurationMs,
    ) {
        buildString {
            append(playbackConfigKey)
            append('|').append(state.animeTitle)
            append('|').append(notificationSubtitle.orEmpty())
            append('|').append(notificationDescription.orEmpty())
            append('|').append(notificationContentText)
            append('|').append(notificationArtworkUrl.orEmpty())
            append('|').append(state.playbackDurationMs.coerceAtLeast(0L))
        }
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

    DisposableEffect(hostView) {
        val wasKeepingScreenOn = hostView.keepScreenOn
        hostView.keepScreenOn = true
        onDispose { hostView.keepScreenOn = wasKeepingScreenOn }
    }

    fun scheduleOverlayHide() {
        hideJob?.cancel()
        hideJob = coroutineScope.launch {
            delay(4.seconds)
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
        if (nextEpisodePromptState.isVisible) {
            return
        }
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
        onVideoTransformChanged(transform)
    }

    val player = mediaController
    val isBuffering = rememberPlayerBufferingState(player)
    val progressSource = remember(player, ui) { ui }

    LaunchedEffect(player, playbackConfigKey, mediaItemKey, ui.activeIframeUrl) {
        val activePlayer = player ?: return@LaunchedEffect
        mediaItemUpdater.update(
            player = activePlayer,
            playbackConfig = playbackConfig,
            config = PlayerMediaItemConfig(
                playbackKey = playbackConfigKey,
                mediaItemKey = mediaItemKey,
                url = currentUrl,
                title = state.animeTitle,
                artist = notificationContentText,
                subtitle = notificationSubtitle,
                description = notificationDescription,
                artworkUrl = notificationArtworkUrl,
                durationMs = state.playbackDurationMs,
                headers = state.streamHeaders,
                offlineCacheKey = state.offlineCacheKey,
                isOfflinePlayback = state.isOfflinePlayback,
                useRotatingHlsCacheKeys = state.isOfflinePlayback &&
                        ui.activeIframeUrl.isAllohaPlayerUrl(),
                audioTrackPolicy = if (ui.activeIframeUrl.isAllohaPlayerUrl()) {
                    PlayerAudioTrackPolicy.FirstAudioGroup
                } else {
                    PlayerAudioTrackPolicy.Default
                },
                playbackPositionMs = state.playbackPositionMs,
                resumeFromMs = state.resumeFromMs,
            ),
        )
        activePlayer.playWhenReady = wantsPlay
    }

    if (player == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        )
        return
    }

    fun saveProgress(positionMs: Long = currentPosition, durationMs: Long = duration) {
        val snapshot = progressSource.buildProgressSnapshot(
            positionMs = positionMs,
            durationMs = durationMs,
        ) ?: return
        onEvent(
            PlayerState.Event.SaveProgress(
                snapshot = snapshot,
            )
        )
        lastSaveTime = System.currentTimeMillis()
    }

    fun notifyPlaybackPositionChanged(positionMs: Long, durationMs: Long) {
        onEvent(
            PlayerState.Event.PlaybackPositionChanged(
                positionMs = positionMs,
                durationMs = durationMs,
                episodeUrl = progressSource.activeIframeUrl,
            )
        )
    }

    fun handleEpisodeEnd(positionMs: Long, durationMs: Long) {
        notifyPlaybackPositionChanged(positionMs, durationMs)
        saveProgress(
            positionMs = positionMs,
            durationMs = durationMs,
        )
        if (!completionReported) {
            completionReported = true
            onEvent(
                PlayerState.Event.EpisodeCompleted(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    episodeUrl = progressSource.activeIframeUrl,
                )
            )
        }
        if (
            ui.hasNextEpisode &&
            !isInPictureInPictureMode &&
            !nextEpisodePromptState.isVisible
        ) {
            nextEpisodePromptState = if (state.autoPlayNextEpisode) {
                PlayerEndPromptState.WithCountdown(
                    PLAYER_END_PROMPT_COUNTDOWN_SECONDS,
                )
            } else {
                PlayerEndPromptState.WithoutCountdown
            }
            overlayVisible = false
            settingsMode = null
            hideJob?.cancel()
        }
    }

    fun seekToPosition(positionMs: Long) {
        val playerDuration = player.duration.takeIf { it > 0 } ?: duration
        val clamped = if (playerDuration > 0) {
            positionMs.coerceIn(0L, playerDuration)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        player.seekTo(clamped)
        if (
            playerDuration > 0L &&
            isAtMobilePlayerEnd(clamped, playerDuration)
        ) {
            handleEpisodeEnd(clamped, playerDuration)
        } else {
            notifyPlaybackPositionChanged(clamped, playerDuration.coerceAtLeast(0L))
            saveProgress(clamped, playerDuration)
        }
    }

    fun stepSeek(direction: MobileSeekDirection) {
        if (direction == MobileSeekDirection.Backward) {
            nextEpisodePromptState = PlayerEndPromptState.Hidden
        }
        val now = System.currentTimeMillis()
        val offset = stepSeekAccumulator.next(direction.toStepSeekDirection(), now)
        seekToPosition(player.currentPosition + offset)
        stepSeekToastText = stepSeekAccumulator.totalOffsetMs.formatSignedSeconds()
        stepSeekToastIcon = direction.toastIcon
        stepSeekToastJob?.cancel()
        stepSeekToastJob = coroutineScope.launch {
            delay(MOBILE_PLAYER_SEEK_TOAST_DURATION)
            stepSeekToastText = null
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                wantsPlay = playWhenReady
                pipSession.setPlaying(playWhenReady, activity)
                if (playWhenReady) scheduleOverlayHide() else hideJob?.cancel()
            }

            override fun onPlayerError(error: PlaybackException) {
                val position = player.currentPosition.coerceAtLeast(0L)
                Log.e(
                    PLAYBACK_LOG_TAG,
                    "Mobile playback error positionMs=$position code=${error.errorCodeName} " +
                            "causes=${error.diagnosticCauseChain()}",
                )
                onEvent(
                    PlayerState.Event.PlaybackError(
                        message = error.localizedMessage
                            ?: error.message
                            ?: error.errorCodeName,
                        errorCode = error.errorCodeName.takeIf { it.isNotBlank() },
                        errorType = error.analyticsType(),
                        positionMs = position,
                    )
                )
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                pipSession.setAspectRatio(videoSize.width, videoSize.height)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    onEvent(PlayerState.Event.PlaybackReady)
                }
                if (playbackState == Player.STATE_ENDED) {
                    val position = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.takeIf { it > 0 } ?: duration
                    handleEpisodeEnd(position, dur)
                }
            }
        }
        player.addListener(listener)
        pipSession.setPlaying(player.playWhenReady, activity)
        pipSession.setCallbacks(
            MobilePlayerPipCallbacks(
                onSeekBackward = {
                    seekToPosition(player.currentPosition - MOBILE_PLAYER_PIP_SEEK_STEP_MS)
                },
                onPlayPause = {
                    if (player.playWhenReady) player.pause() else player.play()
                },
                onSeekForward = {
                    seekToPosition(player.currentPosition + MOBILE_PLAYER_PIP_SEEK_STEP_MS)
                },
            )
        )
        if (wantsPlay) scheduleOverlayHide()
        onDispose {
            hideJob?.cancel()
            stepSeekToastJob?.cancel()
            pipSession.setPlaying(false, activity)
            pipSession.setCallbacks(null)
            val position = player.currentPosition.coerceAtLeast(0)
            val dur = player.duration.takeIf { it > 0 } ?: duration
            notifyPlaybackPositionChanged(position, dur)
            saveProgress(position, dur)
            player.removeListener(listener)
            if (!pipSession.shouldKeepPlayingOnPause()) {
                player.clearMediaItems()
                player.stop()
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (nextEpisodePromptState is PlayerEndPromptState.WithCountdown) {
                        nextEpisodePromptState = PlayerEndPromptState.WithoutCountdown
                    }
                    val keepPlayingInPip = pipSession.shouldKeepPlayingOnPause()
                    resumeAfterLifecyclePause = wantsPlay && !keepPlayingInPip
                    val position = player.currentPosition.coerceAtLeast(0)
                    val dur = player.duration.takeIf { it > 0 } ?: duration
                    notifyPlaybackPositionChanged(position, dur)
                    saveProgress(position, dur)
                    if (!keepPlayingInPip) {
                        player.pause()
                    }
                }

                Lifecycle.Event.ON_RESUME -> if (resumeAfterLifecyclePause) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isInPictureInPictureMode) {
        if (isInPictureInPictureMode) {
            overlayVisible = false
            nextEpisodePromptState = PlayerEndPromptState.Hidden
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

    LaunchedEffect(ui.activeIframeUrl) {
        nextEpisodePromptState = PlayerEndPromptState.Hidden
        settingsMode = null
    }

    LaunchedEffect(nextEpisodePromptState, ui.activeIframeUrl) {
        val countdown = nextEpisodePromptState as? PlayerEndPromptState.WithCountdown
            ?: return@LaunchedEffect
        if (countdown.seconds <= 0) {
            withFrameNanos { }
            nextEpisodePromptState = PlayerEndPromptState.Hidden
            onEvent(PlayerState.Event.NextEpisode(PlayerNextEpisodeSource.EndPrompt))
        } else {
            delay(1.seconds)
            nextEpisodePromptState = PlayerEndPromptState.WithCountdown(
                countdown.seconds - 1,
            )
        }
    }

    BackHandler(enabled = nextEpisodePromptState.isVisible && !isInPictureInPictureMode) {
        nextEpisodePromptState = PlayerEndPromptState.Hidden
        showOverlay()
    }

    LaunchedEffect(videoTransform) {
        if (!transformGestureActive) {
            liveVideoTransform = videoTransform
        }
    }

    LaunchedEffect(player, selectedSpeed, wantsPlay) {
        player.setPlaybackSpeed(selectedSpeed)
        player.playWhenReady = wantsPlay
        pipSession.setPlaying(wantsPlay, activity)
    }

    LaunchedEffect(player, ui.activeIframeUrl, state.autoSkipOpeningsEndings) {
        while (true) {
            var position = currentPosition
            var dur = duration
            if (!isSeeking) {
                position = player.currentPosition.coerceAtLeast(0)
                dur = player.duration.takeIf { it > 0 } ?: 0L
                notifyPlaybackPositionChanged(position, dur)
            }
            bufferedProgress = calculateBufferedProgress(
                bufferedPosition = player.bufferedPosition,
                currentPosition = position,
                duration = dur,
            )
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
                        player.seekTo(segment.endMs)
                        notifyPlaybackPositionChanged(segment.endMs, dur)
                    }
                }
            }
            delay(1.seconds)
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
                        scaleX = liveVideoTransform.scale
                        scaleY = liveVideoTransform.scale
                        translationX = liveVideoTransform.offset.x
                        translationY = liveVideoTransform.offset.y
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

        if (isBuffering || state.isAllohaPlaybackRecovering) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.align(Alignment.Center),
            )
        }

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
            onDetails = { onEvent(PlayerState.Event.OpenDetails) },
            onPictureInPicture = { activity?.let(pipSession::enter) },
            showDetails = state.animeId > 0,
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
            bufferedProgress = bufferedProgress,
            hasPrevEpisode = ui.hasPrevEpisode,
            hasNextEpisode = ui.hasNextEpisode,
            onPlayPause = {
                if (wantsPlay) player.pause() else player.play()
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
                    seekToPosition(newPosition)
                }
                isSeeking = false
                showOverlay()
            },
            onPrevEpisode = { onEvent(PlayerState.Event.PrevEpisode) },
            onNextEpisode = {
                nextEpisodePromptState = PlayerEndPromptState.Hidden
                onEvent(PlayerState.Event.NextEpisode(PlayerNextEpisodeSource.Controls))
            },
            onTrackSettings = {
                nextEpisodePromptState = PlayerEndPromptState.Hidden
                settingsMode = MobilePlayerSettingsMode.Track
                overlayVisible = true
                hideJob?.cancel()
            },
            onPlaybackSettings = {
                nextEpisodePromptState = PlayerEndPromptState.Hidden
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

        if (nextEpisodePromptState.isVisible && ui.hasNextEpisode && !isInPictureInPictureMode) {
            MobilePlayerEndPrompt(
                title = when (val prompt = nextEpisodePromptState) {
                    is PlayerEndPromptState.WithCountdown -> stringResource(
                        R.string.player_next_episode_prompt_countdown,
                        prompt.seconds,
                    )

                    else -> stringResource(R.string.player_next_episode_prompt)
                },
                primaryLabel = stringResource(R.string.player_watch_next),
                stayLabel = stringResource(R.string.player_stay),
                onPrimary = {
                    nextEpisodePromptState = PlayerEndPromptState.Hidden
                    onEvent(PlayerState.Event.NextEpisode(PlayerNextEpisodeSource.EndPrompt))
                },
                onStay = {
                    nextEpisodePromptState = PlayerEndPromptState.Hidden
                    showOverlay()
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
                    saveProgress(position)
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
            )
        }
    }
}

private const val PLAYBACK_LOG_TAG = "PlayerPlayback"
