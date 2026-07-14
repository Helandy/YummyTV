package su.afk.yummy.tv.feature.player

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.player.model.MobilePlayerUiState
import su.afk.yummy.tv.feature.player.model.MobileVideoTransform
import su.afk.yummy.tv.feature.player.pip.MobilePlayerPipController
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.view.MobileNativePlayer
import su.afk.yummy.tv.feature.player.view.MobilePlayerBalancerSheet
import su.afk.yummy.tv.feature.player.view.MobilePlayerDubbingSheet
import su.afk.yummy.tv.feature.player.view.PlayerMessage

@OptIn(UnstableApi::class)
@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun PlayerMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        PlayerMobileScreen(PlayerState.State(), emptyFlow()) {}
    }

@Composable
fun PlayerMobileScreen(
    state: PlayerState.State,
    effect: Flow<PlayerState.Effect>,
    onEvent: (PlayerState.Event) -> Unit,

    ) {
    var showErrorBalancerSheet by rememberSaveable { mutableStateOf(false) }
    var showErrorDubbingSheet by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(effect, context) {
        effect.collect { playerEffect ->
            when (playerEffect) {
                is PlayerState.Effect.ShowMessage ->
                    Toast.makeText(context, playerEffect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    HideMobilePlayerStatusBar()

    BackHandler {
        if (showErrorBalancerSheet) {
            showErrorBalancerSheet = false
        } else if (showErrorDubbingSheet) {
            showErrorDubbingSheet = false
        } else {
            onEvent(PlayerState.Event.Back)
        }
    }

    val videoTransform = MobileVideoTransform(
        scale = state.mobileVideoScale,
        offset = Offset(state.mobileVideoOffsetX, state.mobileVideoOffsetY),
    )
    val playerNamePrefix = stringResource(R.string.player_name_prefix)
    val uiState = remember(state, playerNamePrefix) {
        MobilePlayerUiState.from(
            state = state,
            playerNamePrefix = playerNamePrefix,
        )
    }
    val canChangePlayer = uiState.balancerNames.size > 1
    val canChangeDubbing = uiState.dubbingNames.size > 1
    val errorResumePositionMs = state.playbackPositionMs.takeIf { it > 0L }
        ?: state.resumeFromMs.takeIf { it > 0L }
        ?: 0L
    val streamUrl = state.streamUrl
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.kodikBlockedError != null -> PlayerMessage(
                title = state.kodikBlockedError,
                onBack = { onEvent(PlayerState.Event.Back) },
            )

            state.playerError != null -> PlayerMessage(
                title = state.playerError,
                actionLabel = stringResource(R.string.player_retry),
                onAction = { onEvent(PlayerState.Event.RetryStream) },
                secondaryActionLabel = if (canChangePlayer) {
                    stringResource(R.string.player_change_player)
                } else {
                    null
                },
                onSecondaryAction = if (canChangePlayer) {
                    { showErrorBalancerSheet = true }
                } else {
                    null
                },
                tertiaryActionLabel = if (canChangeDubbing) {
                    stringResource(R.string.player_change_dubbing)
                } else {
                    null
                },
                onTertiaryAction = if (canChangeDubbing) {
                    { showErrorDubbingSheet = true }
                } else {
                    null
                },
                onBack = { onEvent(PlayerState.Event.Back) },
            )

            streamUrl != null -> MobileNativePlayer(
                state = state,
                streamUrl = streamUrl,
                videoTransform = videoTransform,
                onVideoTransformChanged = { transform ->
                    onEvent(
                        PlayerState.Event.MobileVideoTransformChanged(
                            scale = transform.scale,
                            offsetX = transform.offset.x,
                            offsetY = transform.offset.y,
                        )
                    )
                },
                onEvent = onEvent,
            )

            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                        Text(stringResource(R.string.player_loading_stream), color = Color.White)
                    }
                    if (state.showChangePlayerHint && canChangePlayer) {
                        OutlinedButton(onClick = { showErrorBalancerSheet = true }) {
                            Text(stringResource(R.string.player_change_player))
                        }
                    }
                }
            }
        }
        if (showErrorBalancerSheet && canChangePlayer) {
            MobilePlayerBalancerSheet(
                balancerNames = uiState.balancerNames,
                balancerAvailability = uiState.balancerAvailability,
                selectedIndex = uiState.currentBalancerIndex,
                metaLabel = stringResource(R.string.player_balancer_meta),
                onBalancerSelected = { index ->
                    val balancerIndex =
                        uiState.availableBalancerIndices.getOrElse(index) { state.sourceSelection.balancerIndex }
                    showErrorBalancerSheet = false
                    onEvent(
                        PlayerState.Event.BalancerSelected(
                            balancerIndex,
                            errorResumePositionMs
                        )
                    )
                },
                onDismiss = { showErrorBalancerSheet = false },
            )
        }
        if (showErrorDubbingSheet && canChangeDubbing) {
            MobilePlayerDubbingSheet(
                dubbingNames = uiState.dubbingNames,
                dubbingAvailability = uiState.dubbingAvailability,
                selectedIndex = uiState.currentDubbingIndex,
                onDubbingSelected = { index ->
                    showErrorDubbingSheet = false
                    onEvent(PlayerState.Event.DubbingSelected(index, errorResumePositionMs))
                },
                onDismiss = { showErrorDubbingSheet = false },
            )
        }
    }
}

@Composable
private fun HideMobilePlayerStatusBar() {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { MobilePlayerPipController.findActivity(context) }

    DisposableEffect(activity, view) {
        val window = activity?.window
            ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val previousBehavior = controller.systemBarsBehavior

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.statusBars())

        onDispose {
            controller.show(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = previousBehavior
        }
    }
}
