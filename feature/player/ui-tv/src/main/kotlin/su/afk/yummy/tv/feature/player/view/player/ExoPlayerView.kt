package su.afk.yummy.tv.feature.player.view.player

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import su.afk.yummy.tv.feature.player.R
import su.afk.yummy.tv.feature.player.view.deriveQualityUrls

@OptIn(UnstableApi::class)
@Composable
internal fun ExoPlayerView(
    streamUrl: String,
    episodeKey: String = "",
    resumeFromMs: Long = 0L,
    onSaveProgress: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> },
    animeTitle: String = "",
    episode: String = "",
    playerName: String = "",
    dubbing: String = "",
    hasPrevEpisode: Boolean = false,
    hasNextEpisode: Boolean = false,
    onPrevEpisode: () -> Unit = {},
    onNextEpisode: () -> Unit = {},
    allDubbingNames: List<String> = emptyList(),
    currentDubbingIndex: Int = 0,
    onDubbingSelected: (dubbingIndex: Int, currentPositionMs: Long) -> Unit = { _, _ -> },
    allBalancerNames: List<String> = emptyList(),
    currentBalancerIndex: Int = 0,
    onBalancerSelected: (balancerIndex: Int, currentPositionMs: Long) -> Unit = { _, _ -> },
    streamHeaders: Map<String, String> = emptyMap(),
    qualityOverrides: LinkedHashMap<String, String>? = null,
) {
    val context = LocalContext.current
    val qualities = remember(streamUrl, qualityOverrides) { qualityOverrides ?: deriveQualityUrls(streamUrl) }
    var selectedQuality by remember(streamUrl, qualityOverrides) { mutableStateOf(qualities.keys.last()) }
    var seekOnSwitch by remember(streamUrl) { mutableLongStateOf(resumeFromMs) }
    var lastSaveTime by remember { mutableLongStateOf(0L) }
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

    val playFocusRequester = remember { FocusRequester() }
    val overlayFocusRequester = remember { FocusRequester() }
    val selectedQualityFocusRequester = remember { FocusRequester() }
    val selectedDubbingFocusRequester = remember { FocusRequester() }
    val selectedBalancerFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }

    fun scheduleHide() {
        hideJob?.cancel()
        hideJob = coroutineScope.launch {
            delay(4_000)
            if (!showDubbingPanel && !showQualityPanel && !showBalancerPanel) {
                controllerVisible = false
            }
        }
    }

    fun onInteraction() {
        controllerVisible = true
        when {
            showDubbingPanel || showQualityPanel || showBalancerPanel -> hideJob?.cancel()
            wantsPlay -> scheduleHide()
            else -> hideJob?.cancel()
        }
    }

    val currentUrl = remember(streamUrl, selectedQuality) { qualities[selectedQuality] ?: streamUrl }

    val exoPlayer = remember(currentUrl) {
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setForceHighestSupportedBitrate(true))
        }
        val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
            if (streamHeaders.isNotEmpty()) setDefaultRequestProperties(streamHeaders)
        }
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(currentUrl))
                seekTo(seekOnSwitch)
                prepare()
            }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceAtLeast(0)
        exoPlayer.seekTo(clamped)
        currentPosition = clamped
        lastSeekTime = System.currentTimeMillis()
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
        }
        exoPlayer.addListener(listener)
        if (wantsPlay) scheduleHide() else hideJob?.cancel()
        onDispose {
            hideJob?.cancel()
            exoPlayer.removeListener(listener)
            if (episodeKey.isNotBlank() && duration > 0) onSaveProgress(currentPosition, duration)
            exoPlayer.release()
        }
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
                onSaveProgress(currentPosition, duration)
                lastSaveTime = now
            }
            delay(500)
        }
    }

    LaunchedEffect(controllerVisible, showQualityPanel, showDubbingPanel) {
        if (controllerVisible && !showQualityPanel && !showDubbingPanel) {
            try { playFocusRequester.requestFocus() } catch (_: Exception) {}
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

    val displayTime = if (isSeeking) (seekProgress * duration).toLong() else currentPosition

    BackHandler(enabled = showQualityPanel || showDubbingPanel || showBalancerPanel) {
        showQualityPanel = false
        showDubbingPanel = false
        showBalancerPanel = false
        onInteraction()
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
                            Key.DirectionLeft -> seekTo(exoPlayer.currentPosition - 30_000L)
                            Key.DirectionRight -> seekTo(exoPlayer.currentPosition + 30_000L)
                            else -> {}
                        }
                        onInteraction()
                        true
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
                        qualityCount = qualities.size,
                        allDubbingNames = allDubbingNames,
                        currentDubbingIndex = currentDubbingIndex,
                        allBalancerNames = allBalancerNames,
                        currentBalancerIndex = currentBalancerIndex,
                        playerName = playerName,
                        dubbing = dubbing,
                        currentQualityLabel = selectedQuality,
                        onInteraction = ::onInteraction,
                        onPrevEpisode = onPrevEpisode,
                        onNextEpisode = onNextEpisode,
                        onToggleQuality = {
                            showQualityPanel = !showQualityPanel
                            showDubbingPanel = false
                            showBalancerPanel = false
                            if (showQualityPanel) hideJob?.cancel() else onInteraction()
                        },
                        onToggleDubbing = {
                            showDubbingPanel = !showDubbingPanel
                            showQualityPanel = false
                            showBalancerPanel = false
                            if (showDubbingPanel) hideJob?.cancel() else onInteraction()
                        },
                        onToggleBalancer = {
                            showBalancerPanel = !showBalancerPanel
                            showDubbingPanel = false
                            showQualityPanel = false
                            if (showBalancerPanel) hideJob?.cancel() else onInteraction()
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
                showQualityPanel = false
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
            itemMeta = { stringResource(R.string.player_dubbing_meta) },
            onItemSelected = { idx ->
                onDubbingSelected(idx, exoPlayer.currentPosition)
                showDubbingPanel = false
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
                showBalancerPanel = false
                onInteraction()
            },
        )
    }
}

internal fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
