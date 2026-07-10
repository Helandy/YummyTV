package su.afk.yummy.tv.feature.player.view

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.res.stringResource
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
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.utils.resolveContinueWatchingImage
import su.afk.yummy.tv.feature.player.PlayerNextEpisodeSource
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.common.PlayerMediaItemFactory
import su.afk.yummy.tv.feature.player.common.StepSeekAccumulator
import su.afk.yummy.tv.feature.player.common.formatSignedSeconds
import su.afk.yummy.tv.feature.player.isAllohaPlayerUrl
import su.afk.yummy.tv.feature.player.model.MobilePlayerSettingsMode
import su.afk.yummy.tv.feature.player.model.MobilePlayerUiState
import su.afk.yummy.tv.feature.player.model.MobileSeekDirection
import su.afk.yummy.tv.feature.player.model.MobileVideoTransform
import su.afk.yummy.tv.feature.player.pip.MobilePlayerPipCallbacks
import su.afk.yummy.tv.feature.player.pip.MobilePlayerPipController
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.service.rememberMobileMediaController
import su.afk.yummy.tv.feature.player.service.rememberMobilePlayerPlaybackConfig
import su.afk.yummy.tv.feature.player.utils.applyMobileVideoTransform
import su.afk.yummy.tv.feature.player.utils.buildProgressSnapshot
import su.afk.yummy.tv.feature.player.utils.calculateMobileVideoTransform
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
    var showNextEpisodePrompt by remember { mutableStateOf(false) }
    var completionReported by remember(ui.activeIframeUrl, streamUrl) { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }
    var bufferedProgress by remember(streamUrl, ui.activeIframeUrl) { mutableFloatStateOf(0f) }
    var playerSize by remember { mutableStateOf(IntSize.Zero) }
    var stepSeekToastText by remember(streamUrl) { mutableStateOf<String?>(null) }
    var stepSeekToastIcon by remember { mutableStateOf(MobileSeekDirection.Forward.toastIcon) }
    val skippedSegments = remember(streamUrl) { mutableStateListOf<String>() }
    val stepSeekAccumulator = remember(streamUrl) { StepSeekAccumulator() }
    val currentUrl = selectedQuality?.let(qualities::get) ?: streamUrl
    val playbackConfigKey = remember(currentUrl, state.streamHeaders) {
        buildString {
            append(currentUrl)
            state.streamHeaders.entries
                .sortedBy { it.key.lowercase() }
                .forEach { (key, value) ->
                    append('|').append(key).append('=').append(value)
                }
        }
    }
    val mediaController = rememberMobileMediaController()
    val playbackConfig = rememberMobilePlayerPlaybackConfig()
    val coroutineScope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }
    var stepSeekToastJob by remember { mutableStateOf<Job?>(null) }
    val playerViewHolder = remember { arrayOfNulls<PlayerView>(1) }
    var liveVideoTransform by remember { mutableStateOf(videoTransform) }
    var transformGestureActive by remember { mutableStateOf(false) }
    var configuredPlaybackKey by remember { mutableStateOf<String?>(null) }
    var configuredMediaItemKey by remember { mutableStateOf<String?>(null) }
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
            playerViewHolder[0] = null
        }
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
        if (showNextEpisodePrompt) {
            showNextEpisodePrompt = false
            showOverlay()
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
        playerViewHolder[0]?.applyMobileVideoTransform(transform.scale, transform.offset)
        onVideoTransformChanged(transform)
    }

    val player = mediaController
    val progressSource = remember(player, ui) { ui }

    LaunchedEffect(player, playbackConfigKey, mediaItemKey, ui.activeIframeUrl) {
        val activePlayer = player ?: return@LaunchedEffect
        val currentMediaUri = activePlayer.currentMediaItem
            ?.localConfiguration
            ?.uri
            ?.toString()
        val mediaItem = PlayerMediaItemFactory.mediaItemFor(
            url = currentUrl,
            title = state.animeTitle,
            artist = notificationContentText,
            subtitle = notificationSubtitle,
            description = notificationDescription,
            artworkUri = notificationArtworkUrl,
            durationMs = state.playbackDurationMs,
            customCacheKey = state.offlineCacheKey,
        )
        playbackConfig.updateStream(
            headers = state.streamHeaders,
            offlineCacheKey = state.offlineCacheKey.takeIf { state.isOfflinePlayback },
            useRotatingHlsCacheKeys = state.isOfflinePlayback &&
                    ui.activeIframeUrl.isAllohaPlayerUrl(),
        )
        if (currentMediaUri != currentUrl || configuredPlaybackKey != playbackConfigKey) {
            val resumePosition = state.playbackPositionMs.takeIf { it > 0L } ?: state.resumeFromMs
            activePlayer.setMediaItem(
                mediaItem,
                resumePosition
            )
            activePlayer.prepare()
            configuredPlaybackKey = playbackConfigKey
            configuredMediaItemKey = mediaItemKey
        } else if (configuredMediaItemKey != mediaItemKey && activePlayer.mediaItemCount > 0) {
            val itemIndex = activePlayer.currentMediaItemIndex
            if (itemIndex >= 0) {
                activePlayer.replaceMediaItem(itemIndex, mediaItem)
            } else {
                val resumePosition =
                    state.playbackPositionMs.takeIf { it > 0L } ?: state.resumeFromMs
                activePlayer.setMediaItem(mediaItem, resumePosition)
                activePlayer.prepare()
            }
            configuredMediaItemKey = mediaItemKey
        }
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

    fun seekToPosition(positionMs: Long) {
        val playerDuration = player.duration.takeIf { it > 0 } ?: duration
        val clamped = if (playerDuration > 0) {
            positionMs.coerceIn(0L, playerDuration)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        player.seekTo(clamped)
        notifyPlaybackPositionChanged(clamped, playerDuration.coerceAtLeast(0L))
        saveProgress(clamped, playerDuration)
    }

    fun stepSeek(direction: MobileSeekDirection) {
        showNextEpisodePrompt = false
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
                onEvent(
                    PlayerState.Event.PlaybackError(
                        message = error.localizedMessage
                            ?: error.message
                            ?: error.errorCodeName,
                        errorCode = error.errorCodeName.takeIf { it.isNotBlank() },
                        errorType = error.analyticsType(),
                    )
                )
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                pipSession.setAspectRatio(videoSize.width, videoSize.height)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    val position = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.takeIf { it > 0 } ?: duration
                    notifyPlaybackPositionChanged(position, dur)
                    saveProgress(
                        positionMs = position,
                        durationMs = dur,
                    )
                    if (!completionReported) {
                        completionReported = true
                        onEvent(
                            PlayerState.Event.EpisodeCompleted(
                                positionMs = position,
                                durationMs = dur,
                                episodeUrl = progressSource.activeIframeUrl,
                            )
                        )
                    }
                    if (ui.hasNextEpisode && !isInPictureInPictureMode) {
                        showNextEpisodePrompt = true
                        overlayVisible = false
                        settingsMode = null
                        hideJob?.cancel()
                    }
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
            showNextEpisodePrompt = false
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
        showNextEpisodePrompt = false
        settingsMode = null
    }

    BackHandler(enabled = showNextEpisodePrompt && !isInPictureInPictureMode) {
        showNextEpisodePrompt = false
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
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    playerViewHolder[0] = this
                    this.player = player
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
                it.player = player
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
                showNextEpisodePrompt = false
                onEvent(PlayerState.Event.NextEpisode(PlayerNextEpisodeSource.Controls))
            },
            onTrackSettings = {
                showNextEpisodePrompt = false
                settingsMode = MobilePlayerSettingsMode.Track
                overlayVisible = true
                hideJob?.cancel()
            },
            onPlaybackSettings = {
                showNextEpisodePrompt = false
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

        if (showNextEpisodePrompt && ui.hasNextEpisode && !isInPictureInPictureMode) {
            MobilePlayerEndPrompt(
                title = stringResource(R.string.player_next_episode_prompt),
                primaryLabel = stringResource(R.string.player_watch_next),
                stayLabel = stringResource(R.string.player_stay),
                onPrimary = {
                    showNextEpisodePrompt = false
                    onEvent(PlayerState.Event.NextEpisode(PlayerNextEpisodeSource.EndPrompt))
                },
                onStay = {
                    showNextEpisodePrompt = false
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

private fun PlaybackException.analyticsType(): String =
    this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "unknown"

private fun calculateBufferedProgress(
    bufferedPosition: Long,
    currentPosition: Long,
    duration: Long,
): Float {
    if (duration <= 0L) return 0f
    val playedProgress = currentPosition.toFloat() / duration
    val loadedProgress = bufferedPosition.coerceAtLeast(0L).toFloat() / duration
    return loadedProgress.coerceIn(playedProgress.coerceIn(0f, 1f), 1f)
}

private const val MOBILE_PLAYER_PIP_SEEK_STEP_MS = 10_000L
