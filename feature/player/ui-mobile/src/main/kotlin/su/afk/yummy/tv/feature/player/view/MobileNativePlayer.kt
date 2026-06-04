package su.afk.yummy.tv.feature.player.view

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import su.afk.yummy.tv.feature.player.PlayerProgressSnapshot
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.model.MobilePlayerUiState
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
    val ui = remember(state) { MobilePlayerUiState.from(state) }
    val qualities = remember(streamUrl, state.streamQualityMap) {
        state.streamQualityMap?.takeIf { it.isNotEmpty() } ?: linkedMapOf("auto" to streamUrl)
    }
    var selectedQuality by remember(streamUrl, qualities) { mutableStateOf(qualities.keys.last()) }
    var selectedSpeed by remember { mutableFloatStateOf(1f) }
    var resumeOnSourceSwitchMs by remember(streamUrl) { mutableLongStateOf(state.resumeFromMs) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var lastSaveTime by remember { mutableLongStateOf(0L) }
    var showSettings by remember { mutableStateOf(false) }
    val skippedSegments = remember(streamUrl) { mutableStateListOf<String>() }
    val currentUrl = qualities[selectedQuality] ?: streamUrl

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
                seekTo(resumeOnSourceSwitchMs)
                playWhenReady = true
                prepare()
            }
    }

    fun saveProgress(positionMs: Long = currentPosition) {
        if (duration <= 0 || ui.activeIframeUrl.isBlank()) return
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
                    durationMs = duration,
                )
            )
        )
        lastSaveTime = System.currentTimeMillis()
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                onEvent(PlayerState.Event.PlaybackError(error.localizedMessage ?: error.errorCodeName))
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            saveProgress(exoPlayer.currentPosition)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    saveProgress(exoPlayer.currentPosition)
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(exoPlayer, selectedSpeed) {
        exoPlayer.setPlaybackSpeed(selectedSpeed)
    }

    LaunchedEffect(exoPlayer, ui.activeIframeUrl, state.autoSkipOpeningsEndings) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.takeIf { it > 0 } ?: 0L
            val now = System.currentTimeMillis()
            if (duration > 0 && now - lastSaveTime >= 10_000L) {
                saveProgress(currentPosition)
            }
            if (state.autoSkipOpeningsEndings) {
                ui.activeSkips.segments().forEach { (key, segment) ->
                    val segmentKey = "${ui.activeIframeUrl}-$key"
                    if (segmentKey !in skippedSegments &&
                        currentPosition in segment.startMs..segment.endMs
                    ) {
                        skippedSegments += segmentKey
                        exoPlayer.seekTo(segment.endMs)
                    }
                }
            }
            delay(1_000)
        }
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
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            update = { it.player = exoPlayer },
        )

        MobilePlayerTopBar(
            title = state.animeTitle,
            subtitle = listOf(ui.activeEpisode, ui.activeDubbing, ui.activeBalancerName)
                .filter { it.isNotBlank() }
                .joinToString(" • "),
            onBack = { onEvent(PlayerState.Event.Back) },
            onSettings = { showSettings = true },
        )

        MobileEpisodeNav(
            modifier = Modifier.align(Alignment.BottomCenter),
            hasPrevEpisode = ui.hasPrevEpisode,
            hasNextEpisode = ui.hasNextEpisode,
            onPrevEpisode = { onEvent(PlayerState.Event.PrevEpisode) },
            onNextEpisode = { onEvent(PlayerState.Event.NextEpisode) },
        )

        if (showSettings) {
            MobilePlayerSettingsDialog(
                qualities = qualities.keys.toList(),
                selectedQuality = selectedQuality,
                onQualitySelected = { quality ->
                    resumeOnSourceSwitchMs = exoPlayer.currentPosition
                    saveProgress(resumeOnSourceSwitchMs)
                    selectedQuality = quality
                },
                speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f),
                selectedSpeed = selectedSpeed,
                onSpeedSelected = { selectedSpeed = it },
                dubbingNames = ui.dubbingNames,
                selectedDubbingIndex = ui.currentDubbingIndex,
                onDubbingSelected = { onEvent(PlayerState.Event.DubbingSelected(it, exoPlayer.currentPosition)) },
                balancerNames = ui.balancerNames,
                selectedBalancerIndex = ui.currentBalancerIndex,
                onBalancerSelected = { onEvent(PlayerState.Event.BalancerSelected(it, exoPlayer.currentPosition)) },
                onDismiss = { showSettings = false },
            )
        }
    }
}
