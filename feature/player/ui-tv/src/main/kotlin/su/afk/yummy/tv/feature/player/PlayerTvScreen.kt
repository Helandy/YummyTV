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
import androidx.compose.runtime.saveable.rememberSaveable
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
import su.afk.yummy.tv.feature.player.model.PlayerControlFocusTarget
import su.afk.yummy.tv.feature.player.model.rememberPlayerScreenUiState
import su.afk.yummy.tv.feature.player.presentation.R
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
    val uiState = rememberPlayerScreenUiState(
        state = state,
        playerNamePrefix = playerNamePrefix,
    )
    val coroutineScope = rememberCoroutineScope()
    var backPressedOnce by remember { mutableStateOf(false) }
    var backToastText by remember { mutableStateOf<String?>(null) }
    var backToastJob by remember { mutableStateOf<Job?>(null) }
    var pendingControlFocusTarget by rememberSaveable { mutableStateOf<PlayerControlFocusTarget?>(null) }

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

    val streamUrl = state.streamUrl
    val kodikBlockedError = state.kodikBlockedError
    val playerError = state.playerError

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            streamUrl != null -> ExoPlayerView(
                streamUrl = streamUrl,
                streamHeaders = state.streamHeaders,
                qualityOverrides = state.streamQualityMap,
                episodeKey = uiState.activeIframeUrl,
                resumeFromMs = state.resumeFromMs,
                onSaveProgress = { snapshot -> onEvent(PlayerState.Event.SaveProgress(snapshot)) },
                animeTitle = state.animeTitle,
                episode = uiState.activeEpisode,
                videoId = uiState.activeVideoId,
                playerName = uiState.activeBalancerName,
                dubbing = uiState.activeDubbing,
                screenshotUrl = uiState.activeScreenshotUrl,
                hasPrevEpisode = uiState.hasPrevEpisode,
                hasNextEpisode = uiState.hasNextEpisode,
                canRateTitleOnEnd = uiState.canRateTitleOnEnd,
                onPrevEpisode = { onEvent(PlayerState.Event.PrevEpisode) },
                onNextEpisode = { onEvent(PlayerState.Event.NextEpisode) },
                onRateTitle = { onEvent(PlayerState.Event.RateTitle) },
                onPlaybackError = { message -> onEvent(PlayerState.Event.PlaybackError(message)) },
                allDubbingNames = uiState.dubbingOptions.names,
                allDubbingEpisodeCounts = uiState.dubbingOptions.episodeCounts,
                allDubbingViews = uiState.dubbingOptions.views,
                allDubbingSourceNames = uiState.dubbingOptions.sourceNames,
                currentDubbingIndex = uiState.currentDubbingIndex,
                onDubbingSelected = { newIdx, currentPosMs ->
                    pendingControlFocusTarget = PlayerControlFocusTarget.Dubbing
                    onEvent(PlayerState.Event.DubbingSelected(newIdx, currentPosMs))
                },
                allBalancerNames = uiState.availableBalancerNames,
                currentBalancerIndex = uiState.currentBalancerIndex,
                onBalancerSelected = { newIdx, currentPosMs ->
                    val balancerIndex = uiState.availableBalancerIndices.getOrElse(newIdx) { state.balancerIndex }
                    pendingControlFocusTarget = PlayerControlFocusTarget.Balancer
                    onEvent(PlayerState.Event.BalancerSelected(balancerIndex, currentPosMs))
                },
                restoreControlFocusTarget = pendingControlFocusTarget,
                onControlFocusRestored = { pendingControlFocusTarget = null },
                skips = uiState.activeSkips,
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
