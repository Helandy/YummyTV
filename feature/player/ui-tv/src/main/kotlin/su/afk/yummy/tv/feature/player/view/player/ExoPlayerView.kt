package su.afk.yummy.tv.feature.player.view.player

import android.content.Intent
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.preferences.settings.PlayerResizeMode
import su.afk.yummy.tv.core.preferences.settings.PlayerZoomLevel
import su.afk.yummy.tv.domain.player.isAllohaPlayerUrl
import su.afk.yummy.tv.feature.player.PlayerNextEpisodeSource
import su.afk.yummy.tv.feature.player.PlayerProgressSnapshot
import su.afk.yummy.tv.feature.player.PlayerSkips
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.common.PLAYER_END_PROMPT_COUNTDOWN_SECONDS
import su.afk.yummy.tv.feature.player.common.PlayerEndPromptState
import su.afk.yummy.tv.feature.player.common.StepSeekAccumulator
import su.afk.yummy.tv.feature.player.common.analyticsType
import su.afk.yummy.tv.feature.player.common.calculateBufferedProgress
import su.afk.yummy.tv.feature.player.common.formatSignedSeconds
import su.afk.yummy.tv.feature.player.common.isVisible
import su.afk.yummy.tv.feature.player.common.service.PlayerAudioTrackPolicy
import su.afk.yummy.tv.feature.player.common.service.PlayerMediaItemConfig
import su.afk.yummy.tv.feature.player.common.service.PlayerMediaItemUpdater
import su.afk.yummy.tv.feature.player.common.service.PlayerMediaSessionService
import su.afk.yummy.tv.feature.player.common.service.rememberPlayerMediaController
import su.afk.yummy.tv.feature.player.common.service.rememberPlayerPlaybackConfig
import su.afk.yummy.tv.feature.player.model.PanelReturnFocusTarget
import su.afk.yummy.tv.feature.player.model.PlayerControlFocusTarget
import su.afk.yummy.tv.feature.player.model.SeekDirection
import su.afk.yummy.tv.feature.player.model.TvPlayerPanel
import su.afk.yummy.tv.feature.player.model.TvProgressSource
import su.afk.yummy.tv.feature.player.model.rememberTvPlayerFocusRequesters
import su.afk.yummy.tv.feature.player.model.rememberTvPlayerPanelsState
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.utils.buildTvProgressSnapshot
import su.afk.yummy.tv.feature.player.utils.currentSkip
import su.afk.yummy.tv.feature.player.utils.formatCompactCount
import su.afk.yummy.tv.feature.player.utils.formatTime
import su.afk.yummy.tv.feature.player.utils.speedLabel
import su.afk.yummy.tv.feature.player.utils.toPlayerControlFocusTarget
import su.afk.yummy.tv.feature.player.utils.toPlayerSkipType
import su.afk.yummy.tv.feature.player.utils.toStepSeekDirection
import su.afk.yummy.tv.feature.player.utils.toastIcon
import su.afk.yummy.tv.feature.player.utils.tvPlayerContentScale
import su.afk.yummy.tv.feature.player.view.deriveQualityUrls
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(UnstableApi::class)
@Composable
internal fun ExoPlayerView(
    streamUrl: String,
    episodeKey: String = "",
    resumeFromMs: Long = 0L,
    isOfflinePlayback: Boolean = false,
    offlineCacheKey: String? = null,
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
    autoPlayNextEpisode: Boolean = false,
) {
    val context = LocalContext.current
    val hostView = LocalView.current
    val qualities = remember(streamUrl, qualityOverrides) {
        qualityOverrides ?: deriveQualityUrls(streamUrl)
    }
    val speeds = remember { listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f) }
    val activeQuality = selectedQuality?.takeIf { it in qualities }
        ?: qualities.keys.lastOrNull()
    val activeSpeed = selectedSpeed.coerceAtLeast(0.1f)
    var seekOnSwitch by remember(streamUrl) { mutableLongStateOf(resumeFromMs) }
    var lastSaveTime by remember { mutableLongStateOf(0L) }
    var lastPositionNotifyTime by remember { mutableLongStateOf(0L) }
    var wantsPlay by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }
    var bufferedProgress by remember(streamUrl, episodeKey) { mutableFloatStateOf(0f) }
    var isBuffering by remember(streamUrl, episodeKey) { mutableStateOf(false) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }
    var controllerVisible by remember { mutableStateOf(true) }
    val panels = rememberTvPlayerPanelsState()
    var nextEpisodePromptState by remember(episodeKey, streamUrl) {
        mutableStateOf<PlayerEndPromptState>(PlayerEndPromptState.Hidden)
    }
    var showRateTitlePrompt by remember { mutableStateOf(false) }
    var completionReported by remember(episodeKey, streamUrl) { mutableStateOf(false) }
    var highlightedSkipKey by remember { mutableStateOf<String?>(null) }
    var skipSnackbarText by remember(streamUrl) { mutableStateOf<String?>(null) }
    val dismissedSkipKeys = remember(streamUrl) { mutableStateListOf<String>() }
    val stepSeekAccumulator = remember(streamUrl) { StepSeekAccumulator() }

    val focus = rememberTvPlayerFocusRequesters()
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
            delay(4.seconds)
            if (!panels.isAnyOpen && !nextEpisodePromptState.isVisible && !showRateTitlePrompt) {
                controllerVisible = false
            }
        }
    }

    fun onInteraction() {
        controllerVisible = true
        when {
            panels.isAnyOpen || nextEpisodePromptState.isVisible || showRateTitlePrompt -> hideJob?.cancel()
            wantsPlay -> scheduleHide()
            else -> hideJob?.cancel()
        }
    }

    val currentUrl = remember(streamUrl, activeQuality, qualities) {
        activeQuality?.let(qualities::get) ?: streamUrl
    }

    val exoPlayer = rememberPlayerMediaController()
    val playbackConfig = rememberPlayerPlaybackConfig()
    val mediaItemUpdater = remember { PlayerMediaItemUpdater() }
    val playbackKey = remember(currentUrl, streamHeaders, offlineCacheKey) {
        buildString {
            append(currentUrl).append('|').append(offlineCacheKey.orEmpty())
            streamHeaders.entries.sortedBy { it.key.lowercase() }
                .forEach { (key, value) -> append('|').append(key).append('=').append(value) }
        }
    }
    val mediaItemKey =
        remember(playbackKey, animeTitle, episode, dubbing, playerName, screenshotUrl, duration) {
            "$playbackKey|$animeTitle|$episode|$dubbing|$playerName|$screenshotUrl|$duration"
        }

    LaunchedEffect(exoPlayer, playbackKey, mediaItemKey, episodeKey) {
        val player = exoPlayer ?: return@LaunchedEffect
        mediaItemUpdater.update(
            player = player,
            playbackConfig = playbackConfig,
            config = PlayerMediaItemConfig(
                playbackKey = playbackKey,
                mediaItemKey = mediaItemKey,
                url = currentUrl,
                title = animeTitle,
                artist = listOf(dubbing, playerName).filter(String::isNotBlank).joinToString(" • "),
                subtitle = episode.takeIf(String::isNotBlank),
                description = playerName.takeIf(String::isNotBlank),
                artworkUrl = screenshotUrl.takeIf(String::isNotBlank),
                durationMs = duration,
                headers = streamHeaders,
                offlineCacheKey = offlineCacheKey,
                isOfflinePlayback = isOfflinePlayback,
                useRotatingHlsCacheKeys = isOfflinePlayback && episodeKey.isAllohaPlayerUrl(),
                audioTrackPolicy = if (episodeKey.isAllohaPlayerUrl()) {
                    PlayerAudioTrackPolicy.FirstAudioGroup
                } else {
                    PlayerAudioTrackPolicy.Default
                },
                playbackPositionMs = seekOnSwitch,
                resumeFromMs = resumeFromMs,
            ),
        )
        player.playWhenReady = wantsPlay
    }

    if (exoPlayer == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
        return
    }

    DisposableEffect(hostView) {
        val wasKeepingScreenOn = hostView.keepScreenOn
        hostView.keepScreenOn = true
        onDispose { hostView.keepScreenOn = wasKeepingScreenOn }
    }
    val progressSource = remember(
        exoPlayer,
        episodeKey,
        episode,
        videoId,
        playerName,
        dubbing,
        screenshotUrl,
    ) {
        TvProgressSource(
            episodeUrl = episodeKey,
            episode = episode,
            videoId = videoId,
            playerName = playerName,
            dubbing = dubbing,
            screenshotUrl = screenshotUrl,
        )
    }

    fun saveProgressIfReady(positionMs: Long = currentPosition, durationMs: Long = duration) {
        val snapshot = buildTvProgressSnapshot(
            episodeKey = progressSource.episodeUrl,
            episode = progressSource.episode,
            videoId = progressSource.videoId,
            playerName = progressSource.playerName,
            dubbing = progressSource.dubbing,
            screenshotUrl = progressSource.screenshotUrl,
            positionMs = positionMs,
            durationMs = durationMs,
        ) ?: return
        onSaveProgress(snapshot)
        lastSaveTime = System.currentTimeMillis()
    }

    fun notifyPlaybackPositionChanged(positionMs: Long, durationMs: Long) {
        onPlayerEvent(
            PlayerState.Event.PlaybackPositionChanged(
                positionMs = positionMs,
                durationMs = durationMs,
                episodeUrl = progressSource.episodeUrl,
            )
        )
    }

    fun seekTo(positionMs: Long) {
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
        notifyPlaybackPositionChanged(clamped, playerDuration)
        saveProgressIfReady(clamped)
    }

    fun stepSeek(direction: SeekDirection) {
        if (direction == SeekDirection.Backward) {
            nextEpisodePromptState = PlayerEndPromptState.Hidden
        }
        val now = System.currentTimeMillis()
        val offset = stepSeekAccumulator.next(direction.toStepSeekDirection(), now)
        seekTo(exoPlayer.currentPosition + offset)
        stepSeekToastText = stepSeekAccumulator.totalOffsetMs.formatSignedSeconds()
        stepSeekToastIcon = direction.toastIcon
        stepSeekToastJob?.cancel()
        stepSeekToastJob = coroutineScope.launch {
            delay(PLAYER_INLINE_TOAST_DURATION)
            stepSeekToastText = null
        }
    }

    fun closePanels(returnFocusTarget: PanelReturnFocusTarget? = null) {
        if (returnFocusTarget != null) pendingPanelReturnFocusTarget = returnFocusTarget
        panels.close()
    }

    fun togglePanel(panel: TvPlayerPanel, returnFocusTarget: PanelReturnFocusTarget) {
        val opened = panels.toggle(panel)
        if (!opened) pendingPanelReturnFocusTarget = returnFocusTarget
        if (opened) hideJob?.cancel() else onInteraction()
    }

    fun exitPanelDown(returnFocusTarget: PanelReturnFocusTarget) {
        closePanels(returnFocusTarget)
        onInteraction()
    }

    fun requestControlFocus(target: PlayerControlFocusTarget): Boolean {
        return runCatching { focus.control(target).requestFocus() }.isSuccess
    }

    fun requestPanelReturnFocus(): Boolean {
        val target = pendingPanelReturnFocusTarget ?: return false
        val restored = requestControlFocus(target.toPlayerControlFocusTarget())
        if (restored) pendingPanelReturnFocusTarget = null
        return restored
    }

    fun playNextEpisode() {
        saveProgressIfReady()
        nextEpisodePromptState = PlayerEndPromptState.Hidden
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
            delay(3.seconds)
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
                    if (nextEpisodePromptState is PlayerEndPromptState.WithCountdown) {
                        nextEpisodePromptState = PlayerEndPromptState.WithoutCountdown
                    }
                    val position = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.takeIf { it > 0 } ?: duration
                    notifyPlaybackPositionChanged(position, dur)
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
                isBuffering = pwr && exoPlayer.playbackState == Player.STATE_BUFFERING
                if (pwr) scheduleHide() else hideJob?.cancel()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING && exoPlayer.playWhenReady
                if (playbackState == Player.STATE_ENDED) {
                    val position = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.takeIf { it > 0 } ?: duration
                    notifyPlaybackPositionChanged(position, dur)
                    saveProgressIfReady(
                        positionMs = position,
                        durationMs = dur,
                    )
                    if (!completionReported) {
                        completionReported = true
                        onPlayerEvent(
                            PlayerState.Event.EpisodeCompleted(
                                positionMs = position,
                                durationMs = dur,
                                episodeUrl = progressSource.episodeUrl,
                            )
                        )
                    }
                    if (hasNextEpisode) {
                        nextEpisodePromptState = if (autoPlayNextEpisode) {
                            PlayerEndPromptState.WithCountdown(
                                PLAYER_END_PROMPT_COUNTDOWN_SECONDS,
                            )
                        } else {
                            PlayerEndPromptState.WithoutCountdown
                        }
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
        isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING && exoPlayer.playWhenReady
        if (wantsPlay) scheduleHide() else hideJob?.cancel()
        onDispose {
            hideJob?.cancel()
            skipSnackbarJob?.cancel()
            stepSeekToastJob?.cancel()
            exoPlayer.removeListener(listener)
            val position = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration.takeIf { it > 0 } ?: duration
            notifyPlaybackPositionChanged(position, dur)
            saveProgressIfReady(
                positionMs = position,
                durationMs = dur,
            )
            runCatching {
                exoPlayer.pause()
                exoPlayer.clearMediaItems()
                context.stopService(Intent(context, PlayerMediaSessionService::class.java))
            }
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
            bufferedProgress = calculateBufferedProgress(
                bufferedPosition = exoPlayer.bufferedPosition,
                currentPosition = currentPosition,
                duration = duration,
            )
            val now = System.currentTimeMillis()
            if (!isSeeking && duration > 0 && now - lastPositionNotifyTime >= 1_000L) {
                notifyPlaybackPositionChanged(currentPosition, duration)
                lastPositionNotifyTime = now
            }
            if (episodeKey.isNotBlank() && duration > 0 && now - lastSaveTime > 10_000L) {
                saveProgressIfReady()
            }
            delay(500.milliseconds)
        }
    }

    LaunchedEffect(
        controllerVisible,
        panels.activePanel,
        nextEpisodePromptState.isVisible,
        showRateTitlePrompt,
        restoreControlFocusTarget,
    ) {
        if (nextEpisodePromptState.isVisible) {
            withFrameNanos { }
            try {
                focus.nextEpisode.requestFocus()
            } catch (_: Exception) {
            }
        } else if (showRateTitlePrompt) {
            withFrameNanos { }
            try {
                focus.rateTitle.requestFocus()
            } catch (_: Exception) {
            }
        } else if (controllerVisible && !panels.isAnyOpen) {
            withFrameNanos { }
            val restoredExternalTarget = restoreControlFocusTarget?.let { target ->
                requestControlFocus(target).also { restored ->
                    if (restored) onControlFocusRestored()
                }
            } ?: false
            if (!restoredExternalTarget && !requestPanelReturnFocus()) {
                try {
                    focus.play.requestFocus()
                } catch (_: Exception) {
                }
            }
        } else if (!controllerVisible) {
            withFrameNanos { }
            try {
                focus.overlay.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(panels.isOpen(TvPlayerPanel.Quality)) {
        if (panels.isOpen(TvPlayerPanel.Quality)) {
            withFrameNanos { }
            try {
                focus.selectedQuality.requestFocus()
            } catch (_: Exception) {
            }
        }
    }
    LaunchedEffect(panels.isOpen(TvPlayerPanel.Dubbing)) {
        if (panels.isOpen(TvPlayerPanel.Dubbing)) {
            withFrameNanos { }
            try {
                focus.selectedDubbing.requestFocus()
            } catch (_: Exception) {
            }
        }
    }
    LaunchedEffect(panels.isOpen(TvPlayerPanel.Balancer)) {
        if (panels.isOpen(TvPlayerPanel.Balancer)) {
            withFrameNanos { }
            try {
                focus.selectedBalancer.requestFocus()
            } catch (_: Exception) {
            }
        }
    }
    LaunchedEffect(panels.isOpen(TvPlayerPanel.Speed)) {
        if (panels.isOpen(TvPlayerPanel.Speed)) {
            withFrameNanos { }
            try {
                focus.selectedSpeed.requestFocus()
            } catch (_: Exception) {
            }
        }
    }
    LaunchedEffect(panels.isOpen(TvPlayerPanel.Resize)) {
        if (panels.isOpen(TvPlayerPanel.Resize)) {
            withFrameNanos { }
            try {
                focus.selectedResize.requestFocus()
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
                focus.skip.requestFocus()
            } catch (_: Exception) {
            }
            delay(10.seconds)
            if (highlightedSkipKey == skip.key) highlightedSkipKey = null
        }
    }

    val displayTime = if (isSeeking) (seekProgress * duration).toLong() else currentPosition

    LaunchedEffect(nextEpisodePromptState, episodeKey) {
        val countdown = nextEpisodePromptState as? PlayerEndPromptState.WithCountdown
            ?: return@LaunchedEffect
        if (countdown.seconds <= 0) {
            withFrameNanos { }
            playNextEpisode()
        } else {
            delay(1.seconds)
            nextEpisodePromptState = PlayerEndPromptState.WithCountdown(
                countdown.seconds - 1,
            )
        }
    }

    BackHandler(
        enabled = panels.isAnyOpen ||
                nextEpisodePromptState.isVisible ||
                showRateTitlePrompt,
    ) {
        nextEpisodePromptState = PlayerEndPromptState.Hidden
        showRateTitlePrompt = false
        closePanels(
            returnFocusTarget = when (panels.activePanel) {
                TvPlayerPanel.Quality -> PanelReturnFocusTarget.Quality
                TvPlayerPanel.Dubbing -> PanelReturnFocusTarget.Dubbing
                TvPlayerPanel.Balancer -> PanelReturnFocusTarget.Balancer
                TvPlayerPanel.Speed -> PanelReturnFocusTarget.Speed
                TvPlayerPanel.Resize -> PanelReturnFocusTarget.Resize
                null -> null
            }
        )
    }

    val resizeModes = PlayerResizeMode.entries.toList()
    val zoomLevels = PlayerZoomLevel.entries.toList()

    Box(modifier = Modifier.fillMaxSize()) {
        ContentFrame(
            player = exoPlayer,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            contentScale = tvPlayerContentScale(resizeMode, zoomLevel),
            shutter = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false },
        )

        if (isBuffering) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (!controllerVisible) {
            PlayerHiddenKeyOverlay(
                focusRequester = focus.overlay,
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
                                focusRequester = focus.skip,
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
                        bufferedProgress = bufferedProgress,
                        currentPosition = currentPosition,
                        playFocusRequester = focus.play,
                        onPlayPause = { if (wantsPlay) exoPlayer.pause() else exoPlayer.play() },
                        onSeekChange = { v -> isSeeking = true; seekProgress = v },
                        onSeekFinished = {
                            if (isSeeking) {
                                seekTo((seekProgress * duration).toLong())
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
                        currentQualityLabel = activeQuality.orEmpty(),
                        qualityFocusRequester = focus.quality,
                        dubbingFocusRequester = focus.dubbing,
                        balancerFocusRequester = focus.balancer,
                        speedFocusRequester = focus.speed,
                        onInteraction = ::onInteraction,
                        onPrevEpisode = onPrevEpisode,
                        onNextEpisode = { onNextEpisode(PlayerNextEpisodeSource.Controls) },
                        onRateTitle = ::rateTitle,
                        onToggleQuality = {
                            togglePanel(TvPlayerPanel.Quality, PanelReturnFocusTarget.Quality)
                        },
                        onToggleDubbing = {
                            togglePanel(TvPlayerPanel.Dubbing, PanelReturnFocusTarget.Dubbing)
                        },
                        onToggleBalancer = {
                            togglePanel(TvPlayerPanel.Balancer, PanelReturnFocusTarget.Balancer)
                        },
                        resizeFocusRequester = focus.resize,
                        onToggleResize = {
                            togglePanel(TvPlayerPanel.Resize, PanelReturnFocusTarget.Resize)
                        },
                        currentSpeedLabel = activeSpeed.speedLabel(),
                        onToggleSpeed = {
                            togglePanel(TvPlayerPanel.Speed, PanelReturnFocusTarget.Speed)
                        },
                    )
                }
            }
        }

        PlayerSelectionPanel(
            visible = panels.isOpen(TvPlayerPanel.Quality),
            title = stringResource(R.string.player_quality_title),
            items = qualities.keys.toList(),
            selectedIndex = qualities.keys.indexOf(activeQuality),
            selectedFocusRequester = focus.selectedQuality,
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
            visible = panels.isOpen(TvPlayerPanel.Dubbing),
            title = stringResource(R.string.player_dubbing_title),
            items = allDubbingNames,
            selectedIndex = currentDubbingIndex,
            selectedFocusRequester = focus.selectedDubbing,
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
            visible = panels.isOpen(TvPlayerPanel.Speed),
            title = stringResource(R.string.player_speed_title),
            items = speeds.map { it.speedLabel() },
            selectedIndex = speeds.indexOf(activeSpeed).coerceAtLeast(0),
            selectedFocusRequester = focus.selectedSpeed,
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
            visible = panels.isOpen(TvPlayerPanel.Resize),
            resizeModes = resizeModes,
            selectedResizeMode = resizeMode,
            zoomLevels = zoomLevels,
            selectedZoomLevel = zoomLevel,
            selectedResizeFocusRequester = focus.selectedResize,
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
            visible = panels.isOpen(TvPlayerPanel.Balancer),
            title = stringResource(R.string.player_balancer_title),
            items = allBalancerNames.map { it.removePrefix(stringResource(R.string.player_name_prefix)) },
            selectedIndex = currentBalancerIndex,
            selectedFocusRequester = focus.selectedBalancer,
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
            visible = nextEpisodePromptState.isVisible && hasNextEpisode,
            title = when (val prompt = nextEpisodePromptState) {
                is PlayerEndPromptState.WithCountdown -> stringResource(
                    R.string.player_next_episode_prompt_countdown,
                    prompt.seconds,
                )

                else -> stringResource(R.string.player_next_episode_prompt)
            },
            primaryLabel = stringResource(R.string.player_watch_next),
            stayLabel = stringResource(R.string.player_stay),
            primaryFocusRequester = focus.nextEpisode,
            onPrimary = ::playNextEpisode,
            onStay = {
                nextEpisodePromptState = PlayerEndPromptState.Hidden
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
            primaryFocusRequester = focus.rateTitle,
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
