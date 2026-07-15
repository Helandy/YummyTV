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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.player.common.rememberPlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.model.PlayerControlFocusTarget
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.view.KodikBlockedOverlay
import su.afk.yummy.tv.feature.player.view.StreamErrorOverlay
import su.afk.yummy.tv.feature.player.view.StreamLoadingView
import su.afk.yummy.tv.feature.player.view.player.ExoPlayerView
import su.afk.yummy.tv.feature.player.view.player.PLAYER_INLINE_TOAST_DURATION
import su.afk.yummy.tv.feature.player.view.player.PlayerInlineToast
import su.afk.yummy.tv.feature.player.view.player.PlayerSelectionPanel
import kotlin.time.Duration.Companion.seconds

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun PlayerTvScreenDefaultPreview() = ScreenPreviewTheme {
    PlayerTvScreen(PlayerState.State(), emptyFlow()) {}
}

@Composable
fun PlayerTvScreen(
    state: PlayerState.State,
    effect: Flow<PlayerState.Effect>,
    onEvent: (PlayerState.Event) -> Unit,
) {
    val pressBackAgainText = stringResource(R.string.player_press_back_again)
    val playerNamePrefix = stringResource(R.string.player_name_prefix)
    val uiState = rememberPlayerPlaybackUiState(
        state = state,
        playerNamePrefix = playerNamePrefix,
    )
    val coroutineScope = rememberCoroutineScope()
    var backPressedOnce by remember { mutableStateOf(false) }
    var backToastText by remember { mutableStateOf<String?>(null) }
    var backToastJob by remember { mutableStateOf<Job?>(null) }
    var pendingControlFocusTarget by rememberSaveable {
        mutableStateOf<PlayerControlFocusTarget?>(
            null
        )
    }
    var showErrorBalancerPanel by rememberSaveable { mutableStateOf(false) }
    var showErrorDubbingPanel by rememberSaveable { mutableStateOf(false) }
    val selectedErrorBalancerFocusRequester = remember { FocusRequester() }
    val selectedErrorDubbingFocusRequester = remember { FocusRequester() }
    val canChangePlayer = uiState.balancerNames.size > 1
    val canChangeDubbing = uiState.dubbingNames.size > 1

    DisposableEffect(Unit) {
        onDispose {
            backToastJob?.cancel()
        }
    }

    LaunchedEffect(showErrorBalancerPanel, canChangePlayer) {
        if (showErrorBalancerPanel && canChangePlayer) {
            withFrameNanos { }
            runCatching { selectedErrorBalancerFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(showErrorDubbingPanel, canChangeDubbing) {
        if (showErrorDubbingPanel && canChangeDubbing) {
            withFrameNanos { }
            runCatching { selectedErrorDubbingFocusRequester.requestFocus() }
        }
    }

    BackHandler {
        if (showErrorBalancerPanel) {
            showErrorBalancerPanel = false
        } else if (showErrorDubbingPanel) {
            showErrorDubbingPanel = false
        } else if (backPressedOnce) {
            onEvent(PlayerState.Event.Back)
        } else {
            backPressedOnce = true
            backToastText = pressBackAgainText
            backToastJob?.cancel()
            backToastJob = coroutineScope.launch {
                delay(PLAYER_INLINE_TOAST_DURATION)
                backToastText = null
            }
            coroutineScope.launch {
                delay(3.seconds)
                backPressedOnce = false
            }
        }
    }

    val streamUrl = state.streamUrl
    val kodikBlockedError = state.kodikBlockedError
    val playerError = state.playerError
    val errorResumePositionMs = state.playbackPositionMs.takeIf { it > 0L }
        ?: state.resumeFromMs.takeIf { it > 0L }
        ?: 0L

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            kodikBlockedError != null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                KodikBlockedOverlay(
                    message = kodikBlockedError,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            playerError != null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                StreamErrorOverlay(
                    message = playerError,
                    modifier = Modifier.align(Alignment.Center),
                    onRetry = { onEvent(PlayerState.Event.RetryStream) },
                    onChangePlayer = if (canChangePlayer) {
                        { showErrorBalancerPanel = true }
                    } else {
                        null
                    },
                    onChangeDubbing = if (canChangeDubbing) {
                        { showErrorDubbingPanel = true }
                    } else {
                        null
                    },
                )
            }

            streamUrl != null -> ExoPlayerView(
                state = state,
                playback = uiState,
                streamUrl = streamUrl,
                restoreControlFocusTarget = pendingControlFocusTarget,
                onControlFocusRestored = { pendingControlFocusTarget = null },
                onDubbingSelected = { newIdx, currentPosMs ->
                    pendingControlFocusTarget = PlayerControlFocusTarget.Dubbing
                    onEvent(PlayerState.Event.DubbingSelected(newIdx, currentPosMs))
                },
                onBalancerSelected = { newIdx, currentPosMs ->
                    val balancerIndex = uiState.availableBalancerIndices.getOrElse(newIdx) {
                        state.sourceSelection.balancerIndex
                    }
                    pendingControlFocusTarget = PlayerControlFocusTarget.Balancer
                    onEvent(PlayerState.Event.BalancerSelected(balancerIndex, currentPosMs))
                },
                onPlayerEvent = onEvent,
            )

            else -> StreamLoadingView(
                onChangePlayer = if (state.showChangePlayerHint && canChangePlayer) {
                    { showErrorBalancerPanel = true }
                } else {
                    null
                },
            )
        }
        PlayerSelectionPanel(
            visible = showErrorBalancerPanel && canChangePlayer,
            title = stringResource(R.string.player_balancer_title),
            items = uiState.balancerNames.map { it.removePrefix(playerNamePrefix) },
            selectedIndex = uiState.currentBalancerIndex,
            selectedFocusRequester = selectedErrorBalancerFocusRequester,
            enabledItems = uiState.balancerAvailability,
            disabledItemMeta = stringResource(R.string.player_episode_unavailable),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 72.dp),
            itemMeta = { stringResource(R.string.player_balancer_meta) },
            onItemSelected = { index ->
                val balancerIndex =
                    uiState.availableBalancerIndices.getOrElse(index) { state.sourceSelection.balancerIndex }
                showErrorBalancerPanel = false
                pendingControlFocusTarget = PlayerControlFocusTarget.Balancer
                onEvent(
                    PlayerState.Event.BalancerSelected(
                        balancerIndex,
                        errorResumePositionMs
                    )
                )
            },
            onExitDown = { showErrorBalancerPanel = false },
        )
        PlayerSelectionPanel(
            visible = showErrorDubbingPanel && canChangeDubbing,
            title = stringResource(R.string.player_dubbing_title),
            items = uiState.dubbingNames,
            selectedIndex = uiState.currentDubbingIndex,
            selectedFocusRequester = selectedErrorDubbingFocusRequester,
            enabledItems = uiState.dubbingAvailability,
            disabledItemMeta = stringResource(R.string.player_episode_unavailable),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 48.dp, bottom = 72.dp),
            onItemSelected = { index ->
                showErrorDubbingPanel = false
                pendingControlFocusTarget = PlayerControlFocusTarget.Dubbing
                onEvent(PlayerState.Event.DubbingSelected(index, errorResumePositionMs))
            },
            onExitDown = { showErrorDubbingPanel = false },
        )
        PlayerInlineToast(
            text = backToastText,
            icon = Icons.Filled.Home,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
        )
    }
}
