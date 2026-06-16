package su.afk.yummy.tv.feature.player.view.player

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.preferences.settings.PlayerResizeMode
import su.afk.yummy.tv.core.preferences.settings.PlayerZoomLevel
import su.afk.yummy.tv.feature.player.PlayerNextEpisodeSource
import su.afk.yummy.tv.feature.player.PlayerProgressSnapshot
import su.afk.yummy.tv.feature.player.PlayerSeekSource
import su.afk.yummy.tv.feature.player.PlayerSkipType
import su.afk.yummy.tv.feature.player.PlayerSkips
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.common.PlayerDataSourceFactory
import su.afk.yummy.tv.feature.player.common.PlayerMediaItemFactory
import su.afk.yummy.tv.feature.player.common.StepSeekAccumulator
import su.afk.yummy.tv.feature.player.common.formatSignedSeconds
import su.afk.yummy.tv.feature.player.model.ActiveSkipType
import su.afk.yummy.tv.feature.player.model.PanelReturnFocusTarget
import su.afk.yummy.tv.feature.player.model.PlayerControlFocusTarget
import su.afk.yummy.tv.feature.player.model.SeekDirection
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.utils.applyTvResizeMode
import su.afk.yummy.tv.feature.player.utils.buildTvProgressSnapshot
import su.afk.yummy.tv.feature.player.utils.currentSkip
import su.afk.yummy.tv.feature.player.utils.formatCompactCount
import su.afk.yummy.tv.feature.player.utils.formatTime
import su.afk.yummy.tv.feature.player.utils.speedLabel
import su.afk.yummy.tv.feature.player.utils.toPlayerControlFocusTarget
import su.afk.yummy.tv.feature.player.utils.toStepSeekDirection
import su.afk.yummy.tv.feature.player.utils.toastIcon
import su.afk.yummy.tv.feature.player.view.deriveQualityUrls

@OptIn(UnstableApi::class)
@Composable
internal fun ExoPlayerView(
    streamUrl: String,
    episodeKey: String = "",
    resumeFromMs: Long = 0L,
    onSaveProgress: (PlayerProgressSnapshot) -> Unit = {},
    onPlayerEvent: (PlayerState.Event) -> Unit = {},
    animeTitle: String = "",
    episode: String = "",
    videoId: Int = 0,
    playerName: String = "",
    dubbing: String = "",
    screenshotUrl: String = "",
    hasPrevEpisode: Boolean = false,
    hasNextEpisode: Boolean = false,
    canRateTitleOnEnd: Boolean = false,
    onPrevEpisode: () -> Unit = {},
    onNextEpisode: (PlayerNextEpisodeSource) -> Unit = {},
    onRateTitle: () -> Unit = {},
    onPlaybackError: (PlayerState.Event.PlaybackError) -> Unit = {},
    allDubbingNames: List<String> = emptyList(),
    allDubbingEpisodeCounts: List<Int> = emptyList(),
    allDubbingViews: List<Int> = emptyList(),
    allDubbingSourceNames: List<String> = emptyList(),
    currentDubbingIndex: Int = 0,
    onDubbingSelected: (dubbingIndex: Int, currentPositionMs: Long) -> Unit = { _, _ -> },
    allBalancerNames: List<String> = emptyList(),
    currentBalancerIndex: Int = 0,
    onBalancerSelected: (balancerIndex: Int, currentPositionMs: Long) -> Unit = { _, _ -> },
    restoreControlFocusTarget: PlayerControlFocusTarget? = null,
    onControlFocusRestored: () -> Unit = {},
    streamHeaders: Map<String, String> = emptyMap(),
    qualityOverrides: LinkedHashMap<String, String>? = null,
    selectedQuality: String? = null,
    onQualitySelected: (quality: String, currentPositionMs: Long) -> Unit = { _, _ -> },
    selectedSpeed: Float = 1f,
    onSpeedSelected: (Float) -> Unit = {},
    resizeMode: PlayerResizeMode = PlayerResizeMode.FIT,
    onResizeModeSelected: (PlayerResizeMode) -> Unit = {},
    zoomLevel: PlayerZoomLevel = PlayerZoomLevel.PERCENT_10,
    onZoomLevelSelected: (PlayerZoomLevel) -> Unit = {},
    skips: PlayerSkips = PlayerSkips.Empty,
    autoSkipOpeningsEndings: Boolean = false,
) {
    val context = LocalContext.current
    val qualities = remember(streamUrl, qualityOverrides) {
        qualityOverrides?.takeIf { it.isNotEmpty() } ?: deriveQualityUrls(streamUrl)
    }
    val speeds = remember { listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f) }
    val activeQuality = selectedQuality?.takeIf { it in qualities } ?: qualities.keys.last()
    val activeSpeed = selectedSpeed.coerceAtLeast(0.1f)
    var seekOnSwitch by remember(streamUrl) { mutableLongStateOf(resumeFromMs) }
    var lastSaveTime by remember { mutableLongStateOf(0L) }
    var lastPositionNotifyTime by remember { mutableLongStateOf(0L) }
    var wantsPlay by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }
    var controllerVisible by remember { mutableStateOf(true) }
    var showQualityPanel by remember { mutableStateOf(false) }
    var showDubbingPanel by remember { mutableStateOf(false) }
    var showBalancerPanel by remember { mutableStateOf(false) }
    var showSpeedPanel by remember { mutableStateOf(false) }
    var showResizePanel by remember { mutableStateOf(false) }
    var showNextEpisodePrompt by remember { mutableStateOf(false) }
    var showRateTitlePrompt by remember { mutableStateOf(false) }
    var completionReported by remember(episodeKey, streamUrl) { mutableStateOf(false) }
    var highlightedSkipKey by remember { mutableStateOf<String?>(null) }
    var skipSnackbarText by remember(streamUrl) { mutableStateOf<String?>(null) }
    val dismissedSkipKeys = remember(streamUrl) { mutableStateListOf<String>() }
    val stepSeekAccumulator = remember(streamUrl) { StepSeekAccumulator() }

    val playFocusRequester = remember { FocusRequester() }
    val qualityButtonFocusRequester = remember { FocusRequester() }
    val dubbingButtonFocusRequester = remember { FocusRequester() }
    val balancerButtonFocusRequester = remember { FocusRequester() }
    val speedButtonFocusRequester = remember { FocusRequester() }
    val resizeButtonFocusRequester = remember { FocusRequester() }
    val overlayFocusRequester = remember { FocusRequester() }
    val selectedQualityFocusRequester = remember { FocusRequester() }
    val selectedDubbingFocusRequester = remember { FocusRequester() }
    val selectedBalancerFocusRequester = remember { FocusRequester() }
    val selectedSpeedFocusRequester = remember { FocusRequester() }
    val selectedResizeFocusRequester = remember { FocusRequester() }
    val skipFocusRequester = remember { FocusRequester() }
    val nextEpisodeFocusRequester = remember { FocusRequester() }
    val rateTitleFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }
    var skipSnackbarJob by remember { mutableStateOf<Job?>(null) }
    var stepSeekToastText by remember { mutableStateOf<String?>(null) }
    var stepSeekToastIcon by remember { mutableStateOf(Icons.Filled.FastForward) }
    var stepSeekToastJob by remember { mutableStateOf<Job?>(null) }
    var pendingPanelReturnFocusTarget by remember { mutableStateOf<PanelReturnFocusTarget?>(null) }

    fun scheduleHide() {
        hideJob?.cancel()
        hideJob = coroutineScope.launch {
            delay(4_000)
            if (!showDubbingPanel && !showQualityPanel && !showBalancerPanel && !showSpeedPanel && !showResizePanel && !showNextEpisodePrompt && !showRateTitlePrompt) {
                controllerVisible = false
            }
        }
    }

    fun onInteraction() {
        controllerVisible = true
        when {
            showDubbingPanel || showQualityPanel || showBalancerPanel || showSpeedPanel || showResizePanel || showNextEpisodePrompt || showRateTitlePrompt -> hideJob?.cancel()
            wantsPlay -> scheduleHide()
            else -> hideJob?.cancel()
        }
    }

    val currentUrl =
        remember(streamUrl, activeQuality, qualities) { qualities[activeQuality] ?: streamUrl }

    val exoPlayer = remember(currentUrl, streamHeaders) {
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setForceHighestSupportedBitrate(true))
        }
        val dataSourceFactory = PlayerDataSourceFactory.create(streamHeaders)
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                setMediaItem(PlayerMediaItemFactory.mediaItemFor(currentUrl))
                seekTo(seekOnSwitch)
                prepare()
            }
    }

    fun saveProgressIfReady(positionMs: Long = currentPosition, durationMs: Long = duration) {
        val snapshot = buildTvProgressSnapshot(
            episodeKey = episodeKey,
            episode = episode,
            videoId = videoId,
            playerName = playerName,
            dubbing = dubbing,
            screenshotUrl = screenshotUrl,
            positionMs = positionMs,
            durationMs = durationMs,
        ) ?: return
        onSaveProgress(snapshot)
        lastSaveTime = System.currentTimeMillis()
    }

    fun seekTo(positionMs: Long, seekSource: PlayerSeekSource? = null) {
        val fromPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
        val playerDuration = duration.coerceAtLeast(0L)
        val clamped =
            if (playerDuration > 0) {
                positionMs.coerceIn(0L, playerDuration)
            } else {
                positionMs.coerceAtLeast(0)
            }
        exoPlayer.seekTo(clamped)
        currentPosition = clamped
        lastSeekTime = System.currentTimeMillis()
        if (seekSource != null && clamped != fromPosition) {
            onPlayerEvent(
                PlayerState.Event.SeekPerformed(
                    fromMs = fromPosition,
                    toMs = clamped,
                    durationMs = playerDuration,
                    source = seekSource,
                )
            )
        }
        onPlayerEvent(PlayerState.Event.PlaybackPositionChanged(clamped, playerDuration))
        saveProgressIfReady(clamped)
    }

    fun stepSeek(direction: SeekDirection) {
        val now = System.currentTimeMillis()
        val offset = stepSeekAccumulator.next(direction.toStepSeekDirection(), now)
        seekTo(exoPlayer.currentPosition + offset, PlayerSeekSource.RemoteStep)
        stepSeekToastText = stepSeekAccumulator.totalOffsetMs.formatSignedSeconds()
        stepSeekToastIcon = direction.toastIcon
        stepSeekToastJob?.cancel()
        stepSeekToastJob = coroutineScope.launch {
            delay(PLAYER_INLINE_TOAST_DURATION_MS)
            stepSeekToastText = null
        }
    }

    fun closePanels(returnFocusTarget: PanelReturnFocusTarget? = null) {
        if (returnFocusTarget != null) pendingPanelReturnFocusTarget = returnFocusTarget
        showQualityPanel = false
        showDubbingPanel = false
        showBalancerPanel = false
        showSpeedPanel = false
        showResizePanel = false
    }

    fun exitPanelDown(returnFocusTarget: PanelReturnFocusTarget) {
        closePanels(returnFocusTarget)
        onInteraction()
    }

    fun requestControlFocus(target: PlayerControlFocusTarget): Boolean {
        val requester = when (target) {
            PlayerControlFocusTarget.Quality -> qualityButtonFocusRequester
            PlayerControlFocusTarget.Dubbing -> dubbingButtonFocusRequester
            PlayerControlFocusTarget.Balancer -> balancerButtonFocusRequester
            PlayerControlFocusTarget.Resize -> resizeButtonFocusRequester
            PlayerControlFocusTarget.Speed -> speedButtonFocusRequester
        }
        return runCatching { requester.requestFocus() }.isSuccess
    }

    fun requestPanelReturnFocus(): Boolean {
        val target = pendingPanelReturnFocusTarget ?: return false
        val restored = requestControlFocus(target.toPlayerControlFocusTarget())
        if (restored) pendingPanelReturnFocusTarget = null
        return restored
    }

    fun playNextEpisode() {
        saveProgressIfReady()
        showNextEpisodePrompt = false
        showRateTitlePrompt = false
        closePanels()
        onNextEpisode(PlayerNextEpisodeSource.EndPrompt)
    }

    fun rateTitle() {
        showRateTitlePrompt = false
        closePanels()
        onRateTitle()
    }

    val activeSkip = currentSkip(skips, currentPosition, dismissedSkipKeys)
    val activeSkipKey = activeSkip?.key

    fun skipActiveSegment(reportSelection: Boolean = true) {
        val skip = activeSkip ?: return
        if (skip.key !in dismissedSkipKeys) dismissedSkipKeys += skip.key
        highlightedSkipKey = null
        val message = context.getString(
            skip.type.skippedMessageRes,
            formatTime(skip.segment.startMs),
            formatTime(skip.segment.endMs),
        )
        skipSnackbarText = message
        skipSnackbarJob?.cancel()
        skipSnackbarJob = coroutineScope.launch {
            delay(3_000)
            if (skipSnackbarText == message) skipSnackbarText = null
        }
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
        seekTo(skip.segment.endMs)
        onInteraction()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val position = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.takeIf { it > 0 } ?: duration
                    onPlayerEvent(PlayerState.Event.PlaybackPositionChanged(position, dur))
                    saveProgressIfReady(
                        positionMs = position,
                        durationMs = dur,
                    )
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> if (wantsPlay) exoPlayer.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(exoPlayer) {
        exoPlayer.playWhenReady = wantsPlay
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(pwr: Boolean, reason: Int) {
                wantsPlay = pwr
                if (pwr) scheduleHide() else hideJob?.cancel()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    val position = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.takeIf { it > 0 } ?: duration
                    onPlayerEvent(PlayerState.Event.PlaybackPositionChanged(position, dur))
                    saveProgressIfReady(
                        positionMs = position,
                        durationMs = dur,
                    )
                    if (!completionReported) {
                        completionReported = true
                        onPlayerEvent(PlayerState.Event.EpisodeCompleted(position, dur))
                    }
                    if (hasNextEpisode) {
                        showNextEpisodePrompt = true
                        controllerVisible = true
                        closePanels()
                        hideJob?.cancel()
                    } else if (canRateTitleOnEnd) {
                        showRateTitlePrompt = true
                        controllerVisible = true
                        closePanels()
                        hideJob?.cancel()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                onPlaybackError(
                    PlayerState.Event.PlaybackError(
                        message = error.localizedMessage
                            ?: error.message
                            ?: error.errorCodeName,
                        errorCode = error.errorCodeName.takeIf { it.isNotBlank() },
                        errorType = error.analyticsType(),
                    )
                )
            }
        }
        exoPlayer.addListener(listener)
        if (wantsPlay) scheduleHide() else hideJob?.cancel()
        onDispose {
            hideJob?.cancel()
            skipSnackbarJob?.cancel()
            stepSeekToastJob?.cancel()
            exoPlayer.removeListener(listener)
            val position = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration.takeIf { it > 0 } ?: duration
            onPlayerEvent(PlayerState.Event.PlaybackPositionChanged(position, dur))
            saveProgressIfReady(
                positionMs = position,
                durationMs = dur,
            )
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer, activeSpeed) {
        exoPlayer.setPlaybackSpeed(activeSpeed)
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            val sinceSeek = System.currentTimeMillis() - lastSeekTime
            if (!isSeeking && sinceSeek > 1_000L) {
                currentPosition = exoPlayer.currentPosition.coerceAtLeast(0)
            }
            val dur = exoPlayer.duration
            if (dur > 0) duration = dur
            val now = System.currentTimeMillis()
            if (!isSeeking && duration > 0 && now - lastPositionNotifyTime >= 1_000L) {
                onPlayerEvent(PlayerState.Event.PlaybackPositionChanged(currentPosition, duration))
                lastPositionNotifyTime = now
            }
            if (episodeKey.isNotBlank() && duration > 0 && now - lastSaveTime > 10_000L) {
                saveProgressIfReady()
            }
            delay(500)
        }
    }

    LaunchedEffect(
        controllerVisible,
        showQualityPanel,
        showDubbingPanel,
        showBalancerPanel,
        showSpeedPanel,
        showResizePanel,
        showNextEpisodePrompt,
        showRateTitlePrompt,
        restoreControlFocusTarget,
    ) {
        if (showNextEpisodePrompt) {
            withFrameNanos { }
            try {
                nextEpisodeFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        } else if (showRateTitlePrompt) {
            withFrameNanos { }
            try {
                rateTitleFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        } else if (controllerVisible && !showQualityPanel && !showDubbingPanel && !showBalancerPanel && !showSpeedPanel && !showResizePanel) {
            withFrameNanos { }
            val restoredExternalTarget = restoreControlFocusTarget?.let { target ->
                requestControlFocus(target).also { restored ->
                    if (restored) onControlFocusRestored()
                }
            } ?: false
            if (!restoredExternalTarget && !requestPanelReturnFocus()) {
                try {
                    playFocusRequester.requestFocus()
                } catch (_: Exception) {
                }
            }
        } else if (!controllerVisible) {
            try {
                overlayFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(showQualityPanel) {
        if (showQualityPanel) {
            withFrameNanos { }
            try {
                selectedQualityFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }
    LaunchedEffect(showDubbingPanel) {
        if (showDubbingPanel) {
            withFrameNanos { }
            try {
                selectedDubbingFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }
    LaunchedEffect(showBalancerPanel) {
        if (showBalancerPanel) {
            withFrameNanos { }
            try {
                selectedBalancerFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }
    LaunchedEffect(showSpeedPanel) {
        if (showSpeedPanel) {
            withFrameNanos { }
            try {
                selectedSpeedFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }
    LaunchedEffect(showResizePanel) {
        if (showResizePanel) {
            withFrameNanos { }
            try {
                selectedResizeFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }
    LaunchedEffect(activeSkipKey, autoSkipOpeningsEndings) {
        val skip = activeSkip ?: return@LaunchedEffect
        if (autoSkipOpeningsEndings) {
            skipActiveSegment(reportSelection = false)
        } else {
            highlightedSkipKey = skip.key
            controllerVisible = true
            hideJob?.cancel()
            withFrameNanos { }
            try {
                skipFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
            delay(10_000)
            if (highlightedSkipKey == skip.key) highlightedSkipKey = null
        }
    }

    val displayTime = if (isSeeking) (seekProgress * duration).toLong() else currentPosition

    BackHandler(
        enabled = showQualityPanel ||
                showDubbingPanel ||
                showBalancerPanel ||
                showSpeedPanel ||
                showResizePanel ||
                showNextEpisodePrompt ||
                showRateTitlePrompt,
    ) {
        showNextEpisodePrompt = false
        showRateTitlePrompt = false
        closePanels(
            returnFocusTarget = when {
                showQualityPanel -> PanelReturnFocusTarget.Quality
                showDubbingPanel -> PanelReturnFocusTarget.Dubbing
                showBalancerPanel -> PanelReturnFocusTarget.Balancer
                showSpeedPanel -> PanelReturnFocusTarget.Speed
                showResizePanel -> PanelReturnFocusTarget.Resize
                else -> null
            }
        )
    }

    val resizeModes = PlayerResizeMode.entries.toList()
    val zoomLevels = PlayerZoomLevel.entries.toList()

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    keepScreenOn = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    applyTvResizeMode(resizeMode, zoomLevel)
                }.also { it.post { it.requestFocus() } }
            },
            update = { pv ->
                if (pv.player !== exoPlayer) pv.player = exoPlayer
                pv.applyTvResizeMode(resizeMode, zoomLevel)
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (!controllerVisible) {
            PlayerHiddenKeyOverlay(
                focusRequester = overlayFocusRequester,
                onSeekBackward = { stepSeek(SeekDirection.Backward) },
                onSeekForward = { stepSeek(SeekDirection.Forward) },
                onInteraction = ::onInteraction,
            )
        }

        PlayerInfoBar(
            visible = controllerVisible,
            animeTitle = animeTitle,
            episode = episode,
            dubbing = dubbing,
            modifier = Modifier.align(Alignment.TopStart),
        )

        PlayerNameBadge(
            visible = controllerVisible,
            playerName = playerName,
            modifier = Modifier.align(Alignment.TopEnd),
        )

        AnimatedVisibility(
            visible = controllerVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (activeSkip != null && !autoSkipOpeningsEndings) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            ControlButton(
                                onClick = { skipActiveSegment() },
                                onFocused = ::onInteraction,
                                focusRequester = skipFocusRequester,
                                primary = highlightedSkipKey == activeSkip.key,
                            ) { color ->
                                Text(
                                    text = stringResource(R.string.player_skip_segment),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = color,
                                )
                            }
                        }
                    }
                    PlayerProgressRow(
                        wantsPlay = wantsPlay,
                        displayTime = displayTime,
                        duration = duration,
                        isSeeking = isSeeking,
                        seekProgress = seekProgress,
                        currentPosition = currentPosition,
                        playFocusRequester = playFocusRequester,
                        onPlayPause = { if (wantsPlay) exoPlayer.pause() else exoPlayer.play() },
                        onSeekChange = { v -> isSeeking = true; seekProgress = v },
                        onSeekFinished = {
                            if (isSeeking) {
                                seekTo(
                                    (seekProgress * duration).toLong(),
                                    PlayerSeekSource.ProgressBar,
                                )
                                isSeeking = false
                            }
                        },
                        onInteraction = ::onInteraction,
                    )
                    PlayerEpisodeRow(
                        hasPrevEpisode = hasPrevEpisode,
                        hasNextEpisode = hasNextEpisode,
                        canRateTitle = canRateTitleOnEnd && !hasNextEpisode,
                        qualityCount = qualities.size,
                        allDubbingNames = allDubbingNames,
                        currentDubbingIndex = currentDubbingIndex,
                        allBalancerNames = allBalancerNames,
                        currentBalancerIndex = currentBalancerIndex,
                        playerName = playerName,
                        dubbing = dubbing,
                        currentQualityLabel = activeQuality,
                        qualityFocusRequester = qualityButtonFocusRequester,
                        dubbingFocusRequester = dubbingButtonFocusRequester,
                        balancerFocusRequester = balancerButtonFocusRequester,
                        speedFocusRequester = speedButtonFocusRequester,
                        onInteraction = ::onInteraction,
                        onPrevEpisode = onPrevEpisode,
                        onNextEpisode = { onNextEpisode(PlayerNextEpisodeSource.Controls) },
                        onRateTitle = ::rateTitle,
                        onToggleQuality = {
                            showQualityPanel = !showQualityPanel
                            showDubbingPanel = false
                            showBalancerPanel = false
                            showSpeedPanel = false
                            showResizePanel = false
                            if (!showQualityPanel) pendingPanelReturnFocusTarget =
                                PanelReturnFocusTarget.Quality
                            if (showQualityPanel) hideJob?.cancel() else onInteraction()
                        },
                        onToggleDubbing = {
                            showDubbingPanel = !showDubbingPanel
                            showQualityPanel = false
                            showBalancerPanel = false
                            showSpeedPanel = false
                            showResizePanel = false
                            if (!showDubbingPanel) pendingPanelReturnFocusTarget =
                                PanelReturnFocusTarget.Dubbing
                            if (showDubbingPanel) hideJob?.cancel() else onInteraction()
                        },
                        onToggleBalancer = {
                            showBalancerPanel = !showBalancerPanel
                            showDubbingPanel = false
                            showQualityPanel = false
                            showSpeedPanel = false
                            showResizePanel = false
                            if (!showBalancerPanel) pendingPanelReturnFocusTarget =
                                PanelReturnFocusTarget.Balancer
                            if (showBalancerPanel) hideJob?.cancel() else onInteraction()
                        },
                        resizeFocusRequester = resizeButtonFocusRequester,
                        onToggleResize = {
                            showResizePanel = !showResizePanel
                            showBalancerPanel = false
                            showDubbingPanel = false
                            showQualityPanel = false
                            showSpeedPanel = false
                            if (!showResizePanel) pendingPanelReturnFocusTarget =
                                PanelReturnFocusTarget.Resize
                            if (showResizePanel) hideJob?.cancel() else onInteraction()
                        },
                        currentSpeedLabel = activeSpeed.speedLabel(),
                        onToggleSpeed = {
                            showSpeedPanel = !showSpeedPanel
                            showBalancerPanel = false
                            showDubbingPanel = false
                            showQualityPanel = false
                            showResizePanel = false
                            if (!showSpeedPanel) pendingPanelReturnFocusTarget =
                                PanelReturnFocusTarget.Speed
                            if (showSpeedPanel) hideJob?.cancel() else onInteraction()
                        },
                    )
                }
            }
        }

        PlayerSelectionPanel(
            visible = showQualityPanel,
            title = stringResource(R.string.player_quality_title),
            items = qualities.keys.toList(),
            selectedIndex = qualities.keys.indexOf(activeQuality),
            selectedFocusRequester = selectedQualityFocusRequester,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 48.dp, bottom = 72.dp),
            itemMeta = { stringResource(R.string.player_quality_meta) },
            onItemSelected = { idx ->
                val quality = qualities.keys.toList()[idx]
                if (quality != activeQuality) {
                    val position = exoPlayer.currentPosition.coerceAtLeast(0L)
                    seekOnSwitch = position
                    saveProgressIfReady(position)
                    onQualitySelected(quality, position)
                }
                closePanels(PanelReturnFocusTarget.Quality)
                onInteraction()
            },
            onExitDown = { exitPanelDown(PanelReturnFocusTarget.Quality) },
        )

        PlayerSelectionPanel(
            visible = showDubbingPanel,
            title = stringResource(R.string.player_dubbing_title),
            items = allDubbingNames,
            selectedIndex = currentDubbingIndex,
            selectedFocusRequester = selectedDubbingFocusRequester,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 72.dp),
            itemMetaContent = { idx, contentColor ->
                val views = allDubbingViews.getOrElse(idx) { 0 }
                val episodeCount = allDubbingEpisodeCounts.getOrElse(idx) { 0 }
                DubbingMetaRow(
                    views = views.formatCompactCount(),
                    episodeCount = episodeCount,
                    sourceNames = allDubbingSourceNames.getOrElse(idx) { "" },
                    contentColor = contentColor,
                )
            },
            onItemSelected = { idx ->
                onDubbingSelected(idx, exoPlayer.currentPosition)
                closePanels(PanelReturnFocusTarget.Dubbing)
                onInteraction()
            },
            onExitDown = { exitPanelDown(PanelReturnFocusTarget.Dubbing) },
        )

        PlayerSelectionPanel(
            visible = showSpeedPanel,
            title = stringResource(R.string.player_speed_title),
            items = speeds.map { it.speedLabel() },
            selectedIndex = speeds.indexOf(activeSpeed).coerceAtLeast(0),
            selectedFocusRequester = selectedSpeedFocusRequester,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 48.dp, bottom = 72.dp),
            itemMeta = { stringResource(R.string.player_speed_meta) },
            onItemSelected = { idx ->
                val speed = speeds[idx]
                if (speed != activeSpeed) onSpeedSelected(speed)
                closePanels(PanelReturnFocusTarget.Speed)
                onInteraction()
            },
            onExitDown = { exitPanelDown(PanelReturnFocusTarget.Speed) },
        )

        PlayerResizeSettingsPanel(
            visible = showResizePanel,
            resizeModes = resizeModes,
            selectedResizeMode = resizeMode,
            zoomLevels = zoomLevels,
            selectedZoomLevel = zoomLevel,
            selectedResizeFocusRequester = selectedResizeFocusRequester,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 48.dp, bottom = 72.dp),
            onResizeModeSelected = { mode ->
                if (mode != resizeMode) onResizeModeSelected(mode)
                onInteraction()
            },
            onZoomLevelSelected = { level ->
                if (level != zoomLevel || resizeMode != PlayerResizeMode.ZOOM) onZoomLevelSelected(
                    level
                )
                onInteraction()
            },
            onExitDown = { exitPanelDown(PanelReturnFocusTarget.Resize) },
        )

        PlayerSelectionPanel(
            visible = showBalancerPanel,
            title = stringResource(R.string.player_balancer_title),
            items = allBalancerNames.map { it.removePrefix(stringResource(R.string.player_name_prefix)) },
            selectedIndex = currentBalancerIndex,
            selectedFocusRequester = selectedBalancerFocusRequester,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 72.dp),
            itemMeta = { stringResource(R.string.player_balancer_meta) },
            onItemSelected = { idx ->
                onBalancerSelected(idx, exoPlayer.currentPosition)
                closePanels(PanelReturnFocusTarget.Balancer)
                onInteraction()
            },
            onExitDown = { exitPanelDown(PanelReturnFocusTarget.Balancer) },
        )

        PlayerSkipSnackbar(
            text = skipSnackbarText,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (controllerVisible) 136.dp else 36.dp),
        )

        PlayerEndPrompt(
            visible = showNextEpisodePrompt && hasNextEpisode,
            title = stringResource(R.string.player_next_episode_prompt),
            primaryLabel = stringResource(R.string.player_watch_next),
            stayLabel = stringResource(R.string.player_stay),
            primaryFocusRequester = nextEpisodeFocusRequester,
            onPrimary = ::playNextEpisode,
            onStay = {
                showNextEpisodePrompt = false
                onInteraction()
            },
            onInteraction = ::onInteraction,
            modifier = Modifier.align(Alignment.Center),
        )

        PlayerEndPrompt(
            visible = showRateTitlePrompt,
            title = stringResource(R.string.player_rate_title_prompt),
            primaryLabel = stringResource(R.string.player_rate_title),
            stayLabel = stringResource(R.string.player_stay),
            primaryFocusRequester = rateTitleFocusRequester,
            onPrimary = ::rateTitle,
            onStay = {
                showRateTitlePrompt = false
                onInteraction()
            },
            onInteraction = ::onInteraction,
            modifier = Modifier.align(Alignment.Center),
        )

        PlayerInlineToast(
            text = stepSeekToastText,
            icon = stepSeekToastIcon,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (controllerVisible) 136.dp else 36.dp),
        )
    }
}

private fun ActiveSkipType.toPlayerSkipType(): PlayerSkipType =
    when (this) {
        ActiveSkipType.Opening -> PlayerSkipType.Opening
        ActiveSkipType.Ending -> PlayerSkipType.Ending
    }

private fun PlaybackException.analyticsType(): String =
    this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "unknown"
