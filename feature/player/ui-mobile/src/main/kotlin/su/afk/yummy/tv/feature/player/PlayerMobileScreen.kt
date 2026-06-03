package su.afk.yummy.tv.feature.player

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.feature.player.presentation.R

@OptIn(UnstableApi::class)
@Composable
fun PlayerMobileScreen(
    state: PlayerState.State,
    effect: Flow<PlayerState.Effect>,
    onEvent: (PlayerState.Event) -> Unit,
) {
    BackHandler { onEvent(PlayerState.Event.Back) }

    val streamUrl = state.streamUrl
    when {
        streamUrl != null -> MobileNativePlayer(state = state, streamUrl = streamUrl, onEvent = onEvent)
        state.kodikBlockedError != null -> PlayerMessage(
            title = state.kodikBlockedError,
            onBack = { onEvent(PlayerState.Event.Back) },
        )
        state.playerError != null -> PlayerMessage(
            title = state.playerError,
            actionLabel = stringResource(R.string.player_retry),
            onAction = { onEvent(PlayerState.Event.RetryStream) },
            onBack = { onEvent(PlayerState.Event.Back) },
        )
        else -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                Text(stringResource(R.string.player_loading_stream), color = Color.White)
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun MobileNativePlayer(
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

@Composable
private fun MobilePlayerSettingsDialog(
    qualities: List<String>,
    selectedQuality: String,
    onQualitySelected: (String) -> Unit,
    speeds: List<Float>,
    selectedSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    dubbingNames: List<String>,
    selectedDubbingIndex: Int,
    onDubbingSelected: (Int) -> Unit,
    balancerNames: List<String>,
    selectedBalancerIndex: Int,
    onBalancerSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки плеера") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SelectionButton(
                        label = "Качество",
                        value = selectedQuality,
                        items = qualities,
                        onSelected = onQualitySelected,
                        modifier = Modifier.weight(1f),
                    )
                    SelectionButton(
                        label = "Скорость",
                        value = "${selectedSpeed}x",
                        items = speeds.map { "${it}x" },
                        onSelected = { label -> label.removeSuffix("x").toFloatOrNull()?.let(onSpeedSelected) },
                        modifier = Modifier.weight(1f),
                    )
                }
                SelectionButton(
                    label = "Озвучка",
                    value = dubbingNames.getOrElse(selectedDubbingIndex) { "-" },
                    items = dubbingNames,
                    onSelected = { selected -> onDubbingSelected(dubbingNames.indexOf(selected).coerceAtLeast(0)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                SelectionButton(
                    label = "Плеер",
                    value = balancerNames.getOrElse(selectedBalancerIndex) { "-" },
                    items = balancerNames,
                    onSelected = { selected -> onBalancerSelected(balancerNames.indexOf(selected).coerceAtLeast(0)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Готово")
            }
        },
    )
}

@Composable
private fun MobilePlayerTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.74f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun MobileEpisodeNav(
    hasPrevEpisode: Boolean,
    hasNextEpisode: Boolean,
    onPrevEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!hasPrevEpisode && !hasNextEpisode) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilledTonalButton(
            enabled = hasPrevEpisode,
            onClick = onPrevEpisode,
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = null)
            Spacer(Modifier.widthIn(min = 8.dp))
            Text("Предыдущая", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FilledTonalButton(
            enabled = hasNextEpisode,
            onClick = onNextEpisode,
            modifier = Modifier.weight(1f),
        ) {
            Text("Следующая", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.widthIn(min = 8.dp))
            Icon(Icons.Filled.SkipNext, contentDescription = null)
        }
    }
}

@Composable
private fun MobilePlayerControls(
    qualities: List<String>,
    selectedQuality: String,
    onQualitySelected: (String) -> Unit,
    speeds: List<Float>,
    selectedSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    dubbingNames: List<String>,
    selectedDubbingIndex: Int,
    onDubbingSelected: (Int) -> Unit,
    balancerNames: List<String>,
    selectedBalancerIndex: Int,
    onBalancerSelected: (Int) -> Unit,
    hasPrevEpisode: Boolean,
    hasNextEpisode: Boolean,
    onPrevEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = hasPrevEpisode, onClick = onPrevEpisode, modifier = Modifier.weight(1f)) {
                Text("Назад")
            }
            Button(enabled = hasNextEpisode, onClick = onNextEpisode, modifier = Modifier.weight(1f)) {
                Text("Дальше")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SelectionButton(
                label = "Качество",
                value = selectedQuality,
                items = qualities,
                onSelected = onQualitySelected,
                modifier = Modifier.weight(1f),
            )
            SelectionButton(
                label = "Скорость",
                value = "${selectedSpeed}x",
                items = speeds.map { "${it}x" },
                onSelected = { label -> label.removeSuffix("x").toFloatOrNull()?.let(onSpeedSelected) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SelectionButton(
                label = "Озвучка",
                value = dubbingNames.getOrElse(selectedDubbingIndex) { "-" },
                items = dubbingNames,
                onSelected = { selected -> onDubbingSelected(dubbingNames.indexOf(selected).coerceAtLeast(0)) },
                modifier = Modifier.weight(1f),
            )
            SelectionButton(
                label = "Плеер",
                value = balancerNames.getOrElse(selectedBalancerIndex) { "-" },
                items = balancerNames,
                onSelected = { selected -> onBalancerSelected(balancerNames.indexOf(selected).coerceAtLeast(0)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SelectionButton(
    label: String,
    value: String,
    items: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Button(
            enabled = items.isNotEmpty(),
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "$label: $value",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            item,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 260.dp),
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(item)
                    },
                )
            }
        }
    }
}

@Composable
private fun PlayerMessage(
    title: String?,
    onBack: () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
        }
        Column(
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title.orEmpty(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

private data class MobilePlayerUiState(
    val activeIframeUrl: String,
    val activeEpisode: String,
    val activeVideoId: Int,
    val activeDubbing: String,
    val activeBalancerName: String,
    val activeScreenshotUrl: String,
    val activeSkips: PlayerSkips,
    val hasPrevEpisode: Boolean,
    val hasNextEpisode: Boolean,
    val dubbingNames: List<String>,
    val currentDubbingIndex: Int,
    val balancerNames: List<String>,
    val currentBalancerIndex: Int,
) {
    companion object {
        fun from(state: PlayerState.State): MobilePlayerUiState {
            val allDubbingNames = if (state.allBalancerDubbingNames.isNotEmpty()) {
                state.allBalancerDubbingNames.getOrElse(state.balancerIndex) { state.allDubbingNames }
            } else {
                state.allDubbingNames
            }
            val allEpisodeUrls = if (state.allBalancerEpisodeUrls.isNotEmpty()) {
                state.allBalancerEpisodeUrls.getOrElse(state.balancerIndex) { state.allDubbingEpisodeUrls }
            } else {
                state.allDubbingEpisodeUrls
            }
            val allEpisodeNumbers = if (state.allBalancerEpisodeNumbers.isNotEmpty()) {
                state.allBalancerEpisodeNumbers.getOrElse(state.balancerIndex) { state.allDubbingEpisodeNumbers }
            } else {
                state.allDubbingEpisodeNumbers
            }
            val allEpisodeVideoIds = if (state.allBalancerEpisodeVideoIds.isNotEmpty()) {
                state.allBalancerEpisodeVideoIds.getOrElse(state.balancerIndex) { state.allDubbingEpisodeVideoIds }
            } else {
                state.allDubbingEpisodeVideoIds
            }
            val allEpisodeSkips = if (state.allBalancerEpisodeSkips.isNotEmpty()) {
                state.allBalancerEpisodeSkips.getOrElse(state.balancerIndex) { state.allDubbingEpisodeSkips }
            } else {
                state.allDubbingEpisodeSkips
            }
            val activeUrls = allEpisodeUrls.getOrElse(state.dubbingIndex) { state.episodeUrls }
            val activeNumbers = allEpisodeNumbers.getOrElse(state.dubbingIndex) { state.episodeNumbers }
            val activeVideoIds = allEpisodeVideoIds.getOrElse(state.dubbingIndex) { state.episodeVideoIds }
            val activeSkips = allEpisodeSkips.getOrElse(state.dubbingIndex) { state.episodeSkips }
            return MobilePlayerUiState(
                activeIframeUrl = activeUrls.getOrElse(state.episodeIndex) { state.iframeUrl },
                activeEpisode = activeNumbers.getOrElse(state.episodeIndex) { state.episode },
                activeVideoId = activeVideoIds.getOrElse(state.episodeIndex) { 0 },
                activeDubbing = allDubbingNames.getOrElse(state.dubbingIndex) { state.dubbing },
                activeBalancerName = state.allBalancerNames.getOrElse(state.balancerIndex) { state.playerName },
                activeScreenshotUrl = state.screenshotUrls.getOrElse(state.episodeIndex) { "" },
                activeSkips = activeSkips.getOrElse(state.episodeIndex) { PlayerSkips.Empty },
                hasPrevEpisode = state.episodeIndex > 0,
                hasNextEpisode = state.episodeIndex < activeUrls.lastIndex,
                dubbingNames = if (state.allBalancerDubbingNames.isNotEmpty()) {
                    state.allBalancerDubbingNames.flatten().distinct()
                } else {
                    state.allDubbingNames
                },
                currentDubbingIndex = if (state.allBalancerDubbingNames.isNotEmpty()) {
                    state.allBalancerDubbingNames.flatten().distinct().indexOf(allDubbingNames.getOrElse(state.dubbingIndex) { state.dubbing })
                        .coerceAtLeast(0)
                } else {
                    state.dubbingIndex
                },
                balancerNames = state.allBalancerNames,
                currentBalancerIndex = state.balancerIndex,
            )
        }
    }
}

private fun PlayerSkips.segments(): List<Pair<String, PlayerSkipSegment>> =
    buildList {
        opening?.let { add("opening" to it) }
        ending?.let { add("ending" to it) }
    }

private fun mediaItemFor(url: String): MediaItem {
    val lower = url.lowercase()
    val mimeType = when {
        ".m3u8" in lower -> MimeTypes.APPLICATION_M3U8
        ".mpd" in lower -> MimeTypes.APPLICATION_MPD
        else -> null
    }
    return MediaItem.Builder()
        .setUri(url)
        .setMimeType(mimeType)
        .build()
}
