package su.afk.yummy.tv.feature.player

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.feature.player.view.KodikBlockedOverlay
import su.afk.yummy.tv.feature.player.view.StreamErrorOverlay
import su.afk.yummy.tv.feature.player.view.StreamLoadingView
import su.afk.yummy.tv.feature.player.view.player.ExoPlayerView

@Composable
fun PlayerTvScreen(
    state: PlayerState.State,
    effect: Flow<PlayerState.Effect>,
    onEvent: (PlayerState.Event) -> Unit,
) {
    val context = LocalContext.current
    val pressBackAgainText = stringResource(R.string.player_press_back_again)
    val coroutineScope = rememberCoroutineScope()
    var backPressedOnce by remember { mutableStateOf(false) }

    BackHandler {
        if (backPressedOnce) {
            onEvent(PlayerState.Event.Back)
        } else {
            backPressedOnce = true
            Toast.makeText(context, pressBackAgainText, Toast.LENGTH_SHORT).show()
            coroutineScope.launch {
                delay(5_000)
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

    val activeAllDubbingViews = if (state.allBalancerDubbingViews.isNotEmpty())
        state.allBalancerDubbingViews.getOrElse(state.balancerIndex) { state.allDubbingViews }
    else state.allDubbingViews

    val activeAllEpisodeSkips = if (state.allBalancerEpisodeSkips.isNotEmpty())
        state.allBalancerEpisodeSkips.getOrElse(state.balancerIndex) { state.allDubbingEpisodeSkips }
    else state.allDubbingEpisodeSkips

    val activeDubbingUrls = activeAllEpisodeUrls.getOrElse(state.dubbingIndex) { state.episodeUrls }
    val activeEpisodeNumbers = activeAllEpisodeNumbers.getOrElse(state.dubbingIndex) { state.episodeNumbers }
    val activeEpisodeSkips = activeAllEpisodeSkips.getOrElse(state.dubbingIndex) { state.episodeSkips }
    val activeDubbing = activeAllDubbingNames.getOrElse(state.dubbingIndex) { state.dubbing }
    val activeIframeUrl = activeDubbingUrls.getOrElse(state.episodeIndex) { state.iframeUrl }
    val activeEpisode = activeEpisodeNumbers.getOrElse(state.episodeIndex) { state.episode }
    val activeSkips = activeEpisodeSkips.getOrElse(state.episodeIndex) { su.afk.yummy.tv.feature.player.PlayerSkips.Empty }
    val activeBalancerName = if (state.allBalancerNames.isNotEmpty())
        state.allBalancerNames.getOrElse(state.balancerIndex) { state.playerName }
    else state.playerName
    val isFinalEpisode = state.isFinalAvailableEpisode(activeEpisode, activeDubbingUrls.size)

    val streamUrl = state.streamUrl
    val kodikBlockedError = state.kodikBlockedError
    val playerError = state.playerError

    when {
        streamUrl != null -> ExoPlayerView(
            streamUrl = streamUrl,
            streamHeaders = state.streamHeaders,
            qualityOverrides = state.streamQualityMap,
            episodeKey = activeIframeUrl,
            resumeFromMs = state.resumeFromMs,
            onSaveProgress = { posMs, durMs ->
                onEvent(PlayerState.Event.SaveProgress(posMs, durMs))
            },
            animeTitle = state.animeTitle,
            episode = activeEpisode,
            playerName = activeBalancerName,
            dubbing = activeDubbing,
            hasPrevEpisode = state.episodeIndex > 0,
            hasNextEpisode = state.episodeIndex < activeDubbingUrls.size - 1,
            canRateTitleOnEnd = isFinalEpisode && state.animeId > 0,
            onPrevEpisode = { onEvent(PlayerState.Event.PrevEpisode) },
            onNextEpisode = { onEvent(PlayerState.Event.NextEpisode) },
            onRateTitle = { onEvent(PlayerState.Event.RateTitle) },
            onPlaybackError = { message -> onEvent(PlayerState.Event.PlaybackError(message)) },
            allDubbingNames = activeAllDubbingNames,
            allDubbingEpisodeNumbers = activeAllEpisodeNumbers,
            allDubbingViews = activeAllDubbingViews,
            currentDubbingIndex = state.dubbingIndex,
            onDubbingSelected = { newIdx, currentPosMs ->
                onEvent(PlayerState.Event.DubbingSelected(newIdx, currentPosMs))
            },
            allBalancerNames = state.allBalancerNames,
            currentBalancerIndex = state.balancerIndex,
            onBalancerSelected = { newIdx, currentPosMs ->
                onEvent(PlayerState.Event.BalancerSelected(newIdx, currentPosMs))
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
