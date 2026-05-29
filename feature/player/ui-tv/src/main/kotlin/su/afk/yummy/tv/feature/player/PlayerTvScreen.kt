package su.afk.yummy.tv.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.feature.player.view.KodikBlockedOverlay
import su.afk.yummy.tv.feature.player.view.StreamErrorOverlay
import su.afk.yummy.tv.feature.player.view.StreamLoadingView
import su.afk.yummy.tv.feature.player.view.player.ExoPlayerView
import su.afk.yummy.tv.feature.player.view.player.PLAYER_INLINE_TOAST_DURATION_MS
import su.afk.yummy.tv.feature.player.view.player.PlayerInlineToast

@Composable
fun PlayerTvScreen(
    state: PlayerState.State,
    effect: Flow<PlayerState.Effect>,
    onEvent: (PlayerState.Event) -> Unit,
) {
    val pressBackAgainText = stringResource(R.string.player_press_back_again)
    val playerNamePrefix = stringResource(R.string.player_name_prefix)
    val coroutineScope = rememberCoroutineScope()
    var backPressedOnce by remember { mutableStateOf(false) }
    var backToastText by remember { mutableStateOf<String?>(null) }
    var backToastJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            backToastJob?.cancel()
        }
    }

    BackHandler {
        if (backPressedOnce) {
            onEvent(PlayerState.Event.Back)
        } else {
            backPressedOnce = true
            backToastText = pressBackAgainText
            backToastJob?.cancel()
            backToastJob = coroutineScope.launch {
                delay(PLAYER_INLINE_TOAST_DURATION_MS)
                backToastText = null
            }
            coroutineScope.launch {
                delay(3_000)
                backPressedOnce = false
            }
        }
    }

    // Derive active values from state
    val activeAllDubbingNames = if (state.allBalancerDubbingNames.isNotEmpty())
        state.allBalancerDubbingNames.getOrElse(state.balancerIndex) { state.allDubbingNames }
    else state.allDubbingNames

    val activeAllEpisodeUrls = if (state.allBalancerEpisodeUrls.isNotEmpty())
        state.allBalancerEpisodeUrls.getOrElse(state.balancerIndex) { state.allDubbingEpisodeUrls }
    else state.allDubbingEpisodeUrls

    val activeAllEpisodeNumbers = if (state.allBalancerEpisodeNumbers.isNotEmpty())
        state.allBalancerEpisodeNumbers.getOrElse(state.balancerIndex) { state.allDubbingEpisodeNumbers }
    else state.allDubbingEpisodeNumbers

    val activeAllEpisodeVideoIds = if (state.allBalancerEpisodeVideoIds.isNotEmpty())
        state.allBalancerEpisodeVideoIds.getOrElse(state.balancerIndex) { state.allDubbingEpisodeVideoIds }
    else state.allDubbingEpisodeVideoIds

    val activeAllDubbingViews = if (state.allBalancerDubbingViews.isNotEmpty())
        state.allBalancerDubbingViews.getOrElse(state.balancerIndex) { state.allDubbingViews }
    else state.allDubbingViews

    val activeAllEpisodeSkips = if (state.allBalancerEpisodeSkips.isNotEmpty())
        state.allBalancerEpisodeSkips.getOrElse(state.balancerIndex) { state.allDubbingEpisodeSkips }
    else state.allDubbingEpisodeSkips

    val activeDubbingUrls = activeAllEpisodeUrls.getOrElse(state.dubbingIndex) { state.episodeUrls }
    val activeEpisodeNumbers = activeAllEpisodeNumbers.getOrElse(state.dubbingIndex) { state.episodeNumbers }
    val activeEpisodeVideoIds = activeAllEpisodeVideoIds.getOrElse(state.dubbingIndex) { state.episodeVideoIds }
    val activeEpisodeSkips = activeAllEpisodeSkips.getOrElse(state.dubbingIndex) { state.episodeSkips }
    val activeDubbing = activeAllDubbingNames.getOrElse(state.dubbingIndex) { state.dubbing }
    val globalDubbingNames = state.globalDubbingNames()
    val globalDubbingEpisodeNumbers = globalDubbingNames.map { state.globalDubbingEpisodeNumbers(it) }
    val globalDubbingViews = globalDubbingNames.map { state.globalDubbingViews(it) }
    val globalDubbingSourceNames = globalDubbingNames.map { state.globalDubbingSourceNames(it, playerNamePrefix) }
    val currentGlobalDubbingIndex = globalDubbingNames.indexOf(activeDubbing).takeIf { it >= 0 } ?: state.dubbingIndex
    val activeIframeUrl = activeDubbingUrls.getOrElse(state.episodeIndex) { state.iframeUrl }
    val activeEpisode = activeEpisodeNumbers.getOrElse(state.episodeIndex) { state.episode }
    val activeVideoId = activeEpisodeVideoIds.getOrElse(state.episodeIndex) { 0 }
    val activeSkips = activeEpisodeSkips.getOrElse(state.episodeIndex) { su.afk.yummy.tv.feature.player.PlayerSkips.Empty }
    val activeScreenshotUrl = state.screenshotUrls.getOrElse(state.episodeIndex) { "" }
    val activeBalancerName = if (state.allBalancerNames.isNotEmpty())
        state.allBalancerNames.getOrElse(state.balancerIndex) { state.playerName }
    else state.playerName
    val availableBalancerIndices = state.availableBalancerIndices(activeDubbing)
    val availableBalancerNames = availableBalancerIndices.map { index ->
        state.allBalancerNames.getOrElse(index) { state.playerName }
    }
    val currentAvailableBalancerIndex = availableBalancerIndices.indexOf(state.balancerIndex).takeIf { it >= 0 } ?: 0
    val isFinalEpisode = state.isFinalAvailableEpisode(activeEpisode, activeDubbingUrls.size)

    val streamUrl = state.streamUrl
    val kodikBlockedError = state.kodikBlockedError
    val playerError = state.playerError

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            streamUrl != null -> ExoPlayerView(
                streamUrl = streamUrl,
                streamHeaders = state.streamHeaders,
                qualityOverrides = state.streamQualityMap,
                episodeKey = activeIframeUrl,
                resumeFromMs = state.resumeFromMs,
                onSaveProgress = { snapshot -> onEvent(PlayerState.Event.SaveProgress(snapshot)) },
                animeTitle = state.animeTitle,
                episode = activeEpisode,
                videoId = activeVideoId,
                playerName = activeBalancerName,
                dubbing = activeDubbing,
                screenshotUrl = activeScreenshotUrl,
                hasPrevEpisode = state.episodeIndex > 0,
                hasNextEpisode = state.episodeIndex < activeDubbingUrls.size - 1,
                canRateTitleOnEnd = isFinalEpisode && state.animeId > 0,
                onPrevEpisode = { onEvent(PlayerState.Event.PrevEpisode) },
                onNextEpisode = { onEvent(PlayerState.Event.NextEpisode) },
                onRateTitle = { onEvent(PlayerState.Event.RateTitle) },
                onPlaybackError = { message -> onEvent(PlayerState.Event.PlaybackError(message)) },
                allDubbingNames = globalDubbingNames.ifEmpty { activeAllDubbingNames },
                allDubbingEpisodeNumbers = globalDubbingEpisodeNumbers.ifEmpty { activeAllEpisodeNumbers },
                allDubbingViews = globalDubbingViews.ifEmpty { activeAllDubbingViews },
                allDubbingSourceNames = globalDubbingSourceNames,
                currentDubbingIndex = currentGlobalDubbingIndex,
                onDubbingSelected = { newIdx, currentPosMs ->
                    onEvent(PlayerState.Event.DubbingSelected(newIdx, currentPosMs))
                },
                allBalancerNames = availableBalancerNames,
                currentBalancerIndex = currentAvailableBalancerIndex,
                onBalancerSelected = { newIdx, currentPosMs ->
                    val balancerIndex = availableBalancerIndices.getOrElse(newIdx) { state.balancerIndex }
                    onEvent(PlayerState.Event.BalancerSelected(balancerIndex, currentPosMs))
                },
                skips = activeSkips,
                autoSkipOpeningsEndings = state.autoSkipOpeningsEndings,
            )
            kodikBlockedError != null -> Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
            ) {
                KodikBlockedOverlay(
                    message = kodikBlockedError,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            playerError != null -> Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
            ) {
                StreamErrorOverlay(
                    message = playerError,
                    modifier = Modifier.align(Alignment.TopEnd),
                    onRetry = { onEvent(PlayerState.Event.RetryStream) },
                )
            }
            else -> StreamLoadingView()
        }
        PlayerInlineToast(
            text = backToastText,
            icon = Icons.Filled.Home,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
        )
    }
}

private fun PlayerState.State.isFinalAvailableEpisode(activeEpisode: String, activeDubbingSize: Int): Boolean {
    val currentNumber = activeEpisode.toIntOrNull()
    if (currentNumber == null) return episodeIndex >= activeDubbingSize - 1
    val maxNumber = buildList {
        addAll(episodeNumbers.mapNotNull { it.toIntOrNull() })
        allDubbingEpisodeNumbers.forEach { episodes -> addAll(episodes.mapNotNull { it.toIntOrNull() }) }
        allBalancerEpisodeNumbers.forEach { balancer ->
            balancer.forEach { episodes -> addAll(episodes.mapNotNull { it.toIntOrNull() }) }
        }
    }.maxOrNull()
    return maxNumber == null || currentNumber >= maxNumber
}

private fun PlayerState.State.globalDubbingNames(): List<String> =
    if (allBalancerDubbingNames.isNotEmpty()) {
        allBalancerDubbingNames.flatten().distinct()
    } else {
        allDubbingNames
    }

private fun PlayerState.State.globalDubbingEpisodeNumbers(dubbingName: String): List<String> {
    if (allBalancerDubbingNames.isEmpty()) {
        val index = allDubbingNames.indexOf(dubbingName).takeIf { it >= 0 } ?: return emptyList()
        return allDubbingEpisodeNumbers.getOrElse(index) { emptyList() }
    }
    return allBalancerDubbingNames.flatMapIndexed { balancerIndex, dubbingNames ->
        val dubbingIndex = dubbingNames.indexOf(dubbingName)
        if (dubbingIndex < 0) {
            emptyList()
        } else {
            allBalancerEpisodeNumbers
                .getOrElse(balancerIndex) { emptyList() }
                .getOrElse(dubbingIndex) { emptyList() }
        }
    }.distinct()
}

private fun PlayerState.State.globalDubbingViews(dubbingName: String): Int {
    if (allBalancerDubbingNames.isEmpty()) {
        val index = allDubbingNames.indexOf(dubbingName).takeIf { it >= 0 } ?: return 0
        return allDubbingViews.getOrElse(index) { 0 }
    }
    return allBalancerDubbingNames.mapIndexedNotNull { balancerIndex, dubbingNames ->
        val dubbingIndex = dubbingNames.indexOf(dubbingName)
        if (dubbingIndex < 0) {
            null
        } else {
            allBalancerDubbingViews
                .getOrElse(balancerIndex) { emptyList() }
                .getOrElse(dubbingIndex) { 0 }
        }
    }.maxOrNull() ?: 0
}

private fun PlayerState.State.globalDubbingSourceNames(
    dubbingName: String,
    playerNamePrefix: String,
): String {
    if (allBalancerDubbingNames.isEmpty()) return playerName.removePrefix(playerNamePrefix)
    return allBalancerDubbingNames.mapIndexedNotNull { index, dubbingNames ->
        allBalancerNames.getOrNull(index)
            ?.takeIf { dubbingName in dubbingNames }
            ?.removePrefix(playerNamePrefix)
    }.joinToString(" • ")
}

private fun PlayerState.State.availableBalancerIndices(dubbingName: String): List<Int> =
    if (allBalancerDubbingNames.isEmpty()) {
        allBalancerNames.indices.toList()
    } else {
        allBalancerDubbingNames.mapIndexedNotNull { index, dubbingNames ->
            index.takeIf { dubbingName in dubbingNames }
        }
    }
