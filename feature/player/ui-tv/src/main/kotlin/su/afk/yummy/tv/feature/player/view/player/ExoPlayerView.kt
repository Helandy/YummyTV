package su.afk.yummy.tv.feature.player.view.player

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
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
import su.afk.yummy.tv.feature.player.PlayerSkipSegment
import su.afk.yummy.tv.feature.player.PlayerSkips
import su.afk.yummy.tv.feature.player.model.PlayerControlFocusTarget
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.view.deriveQualityUrls
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
internal fun ExoPlayerView(
    streamUrl: String,
    episodeKey: String = "",
    resumeFromMs: Long = 0L,
    onSaveProgress: (PlayerProgressSnapshot) -> Unit = {},
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
    onNextEpisode: () -> Unit = {},
    onRateTitle: () -> Unit = {},
    onPlaybackError: (String) -> Unit = {},
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
    skips: PlayerSkips = PlayerSkips.Empty,
    autoSkipOpeningsEndings: Boolean = false,
) {
    val context = LocalContext.current
    val qualities = remember(streamUrl, qualityOverrides) { qualityOverrides ?: deriveQualityUrls(streamUrl) }
    val speeds = remember { listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f) }
    var selectedQuality by remember(streamUrl, qualityOverrides) { mutableStateOf(qualities.keys.last()) }
    var selectedSpeed by remember { mutableFloatStateOf(1f) }
    var seekOnSwitch by remember(streamUrl) { mutableLongStateOf(resumeFromMs) }
    var lastSaveTime by remember { mutableLongStateOf(0L) }
    var wantsPlay by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }
    var lastStepSeekTime by remember { mutableLongStateOf(0L) }
    var stepSeekCount by remember { mutableStateOf(0) }
    var stepSeekTotalOffset by remember { mutableLongStateOf(0L) }
    var lastStepSeekDirection by remember { mutableStateOf<SeekDirection?>(null) }
    var controllerVisible by remember { mutableStateOf(true) }
    var showQualityPanel by remember { mutableStateOf(false) }
    var showDubbingPanel by remember { mutableStateOf(false) }
    var showBalancerPanel by remember { mutableStateOf(false) }
    var showSpeedPanel by remember { mutableStateOf(false) }
    var showNextEpisodePrompt by remember { mutableStateOf(false) }
    var showRateTitlePrompt by remember { mutableStateOf(false) }
    var highlightedSkipKey by remember { mutableStateOf<String?>(null) }
    var skipSnackbarText by remember(streamUrl) { mutableStateOf<String?>(null) }
    val dismissedSkipKeys = remember(streamUrl) { mutableStateListOf<String>() }

    val playFocusRequester = remember { FocusRequester() }
    val qualityButtonFocusRequester = remember { FocusRequester() }
    val dubbingButtonFocusRequester = remember { FocusRequester() }
    val balancerButtonFocusRequester = remember { FocusRequester() }
    val speedButtonFocusRequester = remember { FocusRequester() }
    val overlayFocusRequester = remember { FocusRequester() }
    val selectedQualityFocusRequester = remember { FocusRequester() }
    val selectedDubbingFocusRequester = remember { FocusRequester() }
    val selectedBalancerFocusRequester = remember { FocusRequester() }
    val selectedSpeedFocusRequester = remember { FocusRequester() }
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
            if (!showDubbingPanel && !showQualityPanel && !showBalancerPanel && !showSpeedPanel && !showNextEpisodePrompt && !showRateTitlePrompt) {
                controllerVisible = false
            }
        }
    }

    fun onInteraction() {
        controllerVisible = true
        when {
            showDubbingPanel || showQualityPanel || showBalancerPanel || showSpeedPanel || showNextEpisodePrompt || showRateTitlePrompt -> hideJob?.cancel()
            wantsPlay -> scheduleHide()
            else -> hideJob?.cancel()
        }
    }

    val currentUrl = remember(streamUrl, selectedQuality) { qualities[selectedQuality] ?: streamUrl }

    val exoPlayer = remember(currentUrl, streamHeaders) {
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setForceHighestSupportedBitrate(true))
        }
        val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
            streamHeaders["User-Agent"]?.takeIf { it.isNotBlank() }?.let(::setUserAgent)
            val requestHeaders = streamHeaders.filterKeys { !it.equals("User-Agent", ignoreCase = true) }
            if (requestHeaders.isNotEmpty()) setDefaultRequestProperties(requestHeaders)
        }
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                setMediaItem(mediaItemFor(currentUrl))
                seekTo(seekOnSwitch)
                prepare()
            }
    }

    fun saveProgressIfReady(positionMs: Long = currentPosition) {
        if (episodeKey.isNotBlank() && duration > 0) {
            onSaveProgress(
                PlayerProgressSnapshot(
                    episode = episode,
                    episodeUrl = episodeKey,
                    videoId = videoId,
                    playerName = playerName,
                    dubbing = dubbing,
                    screenshotUrl = screenshotUrl,
                    positionMs = positionMs,
                    durationMs = duration,
                )
            )
            lastSaveTime = System.currentTimeMillis()
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = if (duration > 0) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0)
        exoPlayer.seekTo(clamped)
        currentPosition = clamped
        lastSeekTime = System.currentTimeMillis()
        saveProgressIfReady(clamped)
    }

    fun stepSeek(direction: SeekDirection) {
        val now = System.currentTimeMillis()
        if (now - lastStepSeekTime > STEP_SEEK_RESET_MS || lastStepSeekDirection != direction) {
            stepSeekCount = 0
            stepSeekTotalOffset = 0L
        }
        stepSeekCount = (stepSeekCount + 1).coerceAtMost(STEP_SEEK_OFFSETS_MS.size)
        lastStepSeekTime = now
        lastStepSeekDirection = direction

        val offset = STEP_SEEK_OFFSETS_MS[stepSeekCount - 1] * direction.sign
        stepSeekTotalOffset += offset
        seekTo(exoPlayer.currentPosition + offset)
        stepSeekToastText = stepSeekTotalOffset.formatSignedSeconds()
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
    }

    fun requestControlFocus(target: PlayerControlFocusTarget): Boolean {
        val requester = when (target) {
            PlayerControlFocusTarget.Quality -> qualityButtonFocusRequester
            PlayerControlFocusTarget.Dubbing -> dubbingButtonFocusRequester
            PlayerControlFocusTarget.Balancer -> balancerButtonFocusRequester
            PlayerControlFocusTarget.Speed -> speedButtonFocusRequester
        }
        return runCatching { requester.requestFocus() }.isSuccess
    }

    fun requestPanelReturnFocus(): Boolean {
        val target = pendingPanelReturnFocusTarget ?: return false
        pendingPanelReturnFocusTarget = null
        return requestControlFocus(target.toPlayerControlFocusTarget())
    }

    fun playNextEpisode() {
        saveProgressIfReady()
        showNextEpisodePrompt = false
        showRateTitlePrompt = false
        closePanels()
        onNextEpisode()
    }

    fun rateTitle() {
        showRateTitlePrompt = false
        closePanels()
        onRateTitle()
    }

    val activeSkip = currentSkip(skips, currentPosition, dismissedSkipKeys)
    val activeSkipKey = activeSkip?.key

    fun skipActiveSegment() {
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
        seekTo(skip.segment.endMs)
        onInteraction()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
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
                    saveProgressIfReady()
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
                onPlaybackError(error.errorCodeName.takeIf { it.isNotBlank() } ?: error.message.orEmpty())
            }
        }
        exoPlayer.addListener(listener)
        if (wantsPlay) scheduleHide() else hideJob?.cancel()
        onDispose {
            hideJob?.cancel()
            skipSnackbarJob?.cancel()
            stepSeekToastJob?.cancel()
            exoPlayer.removeListener(listener)
            saveProgressIfReady()
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer, selectedSpeed) {
        exoPlayer.setPlaybackSpeed(selectedSpeed)
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
        showNextEpisodePrompt,
        showRateTitlePrompt,
        restoreControlFocusTarget,
    ) {
        if (showNextEpisodePrompt) {
            delay(50)
            try { nextEpisodeFocusRequester.requestFocus() } catch (_: Exception) {}
        } else if (showRateTitlePrompt) {
            delay(50)
            try { rateTitleFocusRequester.requestFocus() } catch (_: Exception) {}
        } else if (controllerVisible && !showQualityPanel && !showDubbingPanel && !showBalancerPanel && !showSpeedPanel) {
            val restoredExternalTarget = restoreControlFocusTarget?.let { target ->
                requestControlFocus(target).also { restored ->
                    if (restored) onControlFocusRestored()
                }
            } ?: false
            if (!restoredExternalTarget && !requestPanelReturnFocus()) {
                try { playFocusRequester.requestFocus() } catch (_: Exception) {}
            }
        } else if (!controllerVisible) {
            try { overlayFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(showQualityPanel) {
        if (showQualityPanel) { delay(50); try { selectedQualityFocusRequester.requestFocus() } catch (_: Exception) {} }
    }
    LaunchedEffect(showDubbingPanel) {
        if (showDubbingPanel) { delay(50); try { selectedDubbingFocusRequester.requestFocus() } catch (_: Exception) {} }
    }
    LaunchedEffect(showBalancerPanel) {
        if (showBalancerPanel) { delay(50); try { selectedBalancerFocusRequester.requestFocus() } catch (_: Exception) {} }
    }
    LaunchedEffect(showSpeedPanel) {
        if (showSpeedPanel) { delay(50); try { selectedSpeedFocusRequester.requestFocus() } catch (_: Exception) {} }
    }
    LaunchedEffect(activeSkipKey, autoSkipOpeningsEndings) {
        val skip = activeSkip ?: return@LaunchedEffect
        if (autoSkipOpeningsEndings) {
            skipActiveSegment()
        } else {
            highlightedSkipKey = skip.key
            controllerVisible = true
            hideJob?.cancel()
            delay(50)
            try { skipFocusRequester.requestFocus() } catch (_: Exception) {}
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
                else -> null
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    keepScreenOn = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }.also { it.post { it.requestFocus() } }
            },
            update = { pv -> if (pv.player !== exoPlayer) pv.player = exoPlayer },
            modifier = Modifier.fillMaxSize(),
        )

        if (!controllerVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(overlayFocusRequester)
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                        when (event.key) {
                            Key.DirectionLeft -> {
                                stepSeek(SeekDirection.Backward)
                                true
                            }
                            Key.DirectionRight -> {
                                stepSeek(SeekDirection.Forward)
                                true
                            }
                            else -> {
                                onInteraction()
                                true
                            }
                        }
                    },
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
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)))),
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
                                onClick = ::skipActiveSegment,
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
                            if (isSeeking) { seekTo((seekProgress * duration).toLong()); isSeeking = false }
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
                        currentQualityLabel = selectedQuality,
                        qualityFocusRequester = qualityButtonFocusRequester,
                        dubbingFocusRequester = dubbingButtonFocusRequester,
                        balancerFocusRequester = balancerButtonFocusRequester,
                        speedFocusRequester = speedButtonFocusRequester,
                        onInteraction = ::onInteraction,
                        onPrevEpisode = onPrevEpisode,
                        onNextEpisode = onNextEpisode,
                        onRateTitle = ::rateTitle,
                        onToggleQuality = {
                            showQualityPanel = !showQualityPanel
                            showDubbingPanel = false
                            showBalancerPanel = false
                            showSpeedPanel = false
                            if (!showQualityPanel) pendingPanelReturnFocusTarget = PanelReturnFocusTarget.Quality
                            if (showQualityPanel) hideJob?.cancel() else onInteraction()
                        },
                        onToggleDubbing = {
                            showDubbingPanel = !showDubbingPanel
                            showQualityPanel = false
                            showBalancerPanel = false
                            showSpeedPanel = false
                            if (!showDubbingPanel) pendingPanelReturnFocusTarget = PanelReturnFocusTarget.Dubbing
                            if (showDubbingPanel) hideJob?.cancel() else onInteraction()
                        },
                        onToggleBalancer = {
                            showBalancerPanel = !showBalancerPanel
                            showDubbingPanel = false
                            showQualityPanel = false
                            showSpeedPanel = false
                            if (!showBalancerPanel) pendingPanelReturnFocusTarget = PanelReturnFocusTarget.Balancer
                            if (showBalancerPanel) hideJob?.cancel() else onInteraction()
                        },
                        currentSpeedLabel = selectedSpeed.speedLabel(),
                        onToggleSpeed = {
                            showSpeedPanel = !showSpeedPanel
                            showBalancerPanel = false
                            showDubbingPanel = false
                            showQualityPanel = false
                            if (!showSpeedPanel) pendingPanelReturnFocusTarget = PanelReturnFocusTarget.Speed
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
            selectedIndex = qualities.keys.indexOf(selectedQuality),
            selectedFocusRequester = selectedQualityFocusRequester,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 48.dp, bottom = 72.dp),
            itemMeta = { stringResource(R.string.player_quality_meta) },
            onItemSelected = { idx ->
                val quality = qualities.keys.toList()[idx]
                if (quality != selectedQuality) {
                    seekOnSwitch = exoPlayer.currentPosition
                    selectedQuality = quality
                }
                closePanels(PanelReturnFocusTarget.Quality)
                onInteraction()
            },
        )

        PlayerSelectionPanel(
            visible = showDubbingPanel,
            title = stringResource(R.string.player_dubbing_title),
            items = allDubbingNames,
            selectedIndex = currentDubbingIndex,
            selectedFocusRequester = selectedDubbingFocusRequester,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 48.dp, bottom = 72.dp),
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
        )

        PlayerSelectionPanel(
            visible = showSpeedPanel,
            title = stringResource(R.string.player_speed_title),
            items = speeds.map { it.speedLabel() },
            selectedIndex = speeds.indexOf(selectedSpeed).coerceAtLeast(0),
            selectedFocusRequester = selectedSpeedFocusRequester,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 48.dp, bottom = 72.dp),
            itemMeta = { stringResource(R.string.player_speed_meta) },
            onItemSelected = { idx ->
                selectedSpeed = speeds[idx]
                closePanels(PanelReturnFocusTarget.Speed)
                onInteraction()
            },
        )

        PlayerSelectionPanel(
            visible = showBalancerPanel,
            title = stringResource(R.string.player_balancer_title),
            items = allBalancerNames.map { it.removePrefix(stringResource(R.string.player_name_prefix)) },
            selectedIndex = currentBalancerIndex,
            selectedFocusRequester = selectedBalancerFocusRequester,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 48.dp, bottom = 72.dp),
            itemMeta = { stringResource(R.string.player_balancer_meta) },
            onItemSelected = { idx ->
                onBalancerSelected(idx, exoPlayer.currentPosition)
                closePanels(PanelReturnFocusTarget.Balancer)
                onInteraction()
            },
        )

        AnimatedVisibility(
            visible = skipSnackbarText != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (controllerVisible) 136.dp else 36.dp),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = skipSnackbarText.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }

        if (showNextEpisodePrompt && hasNextEpisode) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(horizontal = 28.dp, vertical = 22.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(R.string.player_next_episode_prompt),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ControlButton(
                            onClick = ::playNextEpisode,
                            onFocused = ::onInteraction,
                            focusRequester = nextEpisodeFocusRequester,
                            primary = true,
                        ) { color ->
                            Text(stringResource(R.string.player_watch_next), style = MaterialTheme.typography.labelLarge, color = color)
                        }
                        ControlButton(
                            onClick = {
                                showNextEpisodePrompt = false
                                onInteraction()
                            },
                            onFocused = ::onInteraction,
                            modifier = Modifier.width(120.dp),
                        ) { color ->
                            Text(stringResource(R.string.player_stay), style = MaterialTheme.typography.labelLarge, color = color)
                        }
                    }
                }
            }
        }

        if (showRateTitlePrompt) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(horizontal = 28.dp, vertical = 22.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(R.string.player_rate_title_prompt),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ControlButton(
                            onClick = ::rateTitle,
                            onFocused = ::onInteraction,
                            focusRequester = rateTitleFocusRequester,
                            primary = true,
                        ) { color ->
                            Text(stringResource(R.string.player_rate_title), style = MaterialTheme.typography.labelLarge, color = color)
                        }
                        ControlButton(
                            onClick = {
                                showRateTitlePrompt = false
                                onInteraction()
                            },
                            onFocused = ::onInteraction,
                            modifier = Modifier.width(120.dp),
                        ) { color ->
                            Text(stringResource(R.string.player_stay), style = MaterialTheme.typography.labelLarge, color = color)
                        }
                    }
                }
            }
        }

        PlayerInlineToast(
            text = stepSeekToastText,
            icon = stepSeekToastIcon,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (controllerVisible) 136.dp else 36.dp),
        )
    }
}

private const val STEP_SEEK_RESET_MS = 1_500L
private val STEP_SEEK_OFFSETS_MS = longArrayOf(5_000L, 10_000L, 15_000L)

private enum class SeekDirection(val sign: Int) {
    Backward(-1),
    Forward(1),
}

private enum class PanelReturnFocusTarget {
    Quality,
    Dubbing,
    Balancer,
    Speed,
}

private fun PanelReturnFocusTarget.toPlayerControlFocusTarget(): PlayerControlFocusTarget =
    when (this) {
        PanelReturnFocusTarget.Quality -> PlayerControlFocusTarget.Quality
        PanelReturnFocusTarget.Dubbing -> PlayerControlFocusTarget.Dubbing
        PanelReturnFocusTarget.Balancer -> PlayerControlFocusTarget.Balancer
        PanelReturnFocusTarget.Speed -> PlayerControlFocusTarget.Speed
    }

private val SeekDirection.toastIcon: ImageVector
    get() = when (this) {
        SeekDirection.Backward -> Icons.Filled.FastRewind
        SeekDirection.Forward -> Icons.Filled.FastForward
    }

private fun Long.formatSignedSeconds(): String {
    val seconds = this / 1_000L
    val prefix = if (seconds > 0) "+" else ""
    return "${prefix}${seconds}s"
}

private fun mediaItemFor(url: String): MediaItem {
    val cleanUrl = url.substringBefore('?').substringBefore('#')
    val mimeType = when {
        cleanUrl.endsWith(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
        cleanUrl.endsWith(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
        else -> null
    }
    return MediaItem.Builder()
        .setUri(url)
        .apply { if (mimeType != null) setMimeType(mimeType) }
        .build()
}

@Composable
private fun DubbingMetaRow(
    views: String,
    episodeCount: Int,
    sourceNames: String,
    contentColor: Color,
) {
    val metaColor = contentColor.copy(alpha = 0.62f)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = null,
                tint = metaColor,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = views,
                style = MaterialTheme.typography.labelSmall,
                color = metaColor,
                maxLines = 1,
                modifier = Modifier.width(42.dp),
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.VideoLibrary,
                contentDescription = null,
                tint = metaColor,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = episodeCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = metaColor,
                maxLines = 1,
                modifier = Modifier.width(24.dp),
            )
        }
        if (sourceNames.isNotBlank()) {
            Text(
                text = sourceNames,
                style = MaterialTheme.typography.labelSmall,
                color = metaColor,
                maxLines = 1,
                modifier = Modifier.widthIn(max = 260.dp),
            )
        }
    }
}

@Composable
private fun Int.formatCompactCount(): String = when {
    this >= 1_000_000 -> stringResource(R.string.player_count_millions, (this / 1_000_000f).formatCompactDecimal())
    this >= 1_000 -> stringResource(R.string.player_count_thousands, (this / 1_000f).formatCompactDecimal())
    else -> toString()
}

private fun Float.formatCompactDecimal(): String =
    if (this % 1f == 0f) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }

internal fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

internal fun Float.speedLabel(): String =
    if (this % 1f == 0f) "${toInt()}x" else "${this}x"

private data class ActiveSkip(
    val key: String,
    val type: ActiveSkipType,
    val segment: PlayerSkipSegment,
)

private enum class ActiveSkipType(@param:StringRes val skippedMessageRes: Int) {
    Opening(R.string.player_opening_skipped),
    Ending(R.string.player_ending_skipped),
}

private fun currentSkip(
    skips: PlayerSkips,
    positionMs: Long,
    dismissedKeys: List<String>,
): ActiveSkip? =
    listOfNotNull(
        skips.opening?.let { ActiveSkip("opening:${it.startMs}:${it.endMs}", ActiveSkipType.Opening, it) },
        skips.ending?.let { ActiveSkip("ending:${it.startMs}:${it.endMs}", ActiveSkipType.Ending, it) },
    ).firstOrNull { skip ->
        skip.key !in dismissedKeys && positionMs in skip.segment.startMs..skip.segment.endMs
    }
