package su.afk.yummy.tv.feature.player.mobile

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.components.YummyLoadingIndicator
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.mapper.loadingTextRes
import su.afk.yummy.tv.feature.player.common.PlayerResolveLogView
import su.afk.yummy.tv.feature.player.common.rememberPlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.common.service.PlayerServiceWarmupEffect
import su.afk.yummy.tv.feature.player.mobile.model.MobileVideoTransform
import su.afk.yummy.tv.feature.player.mobile.pip.MobilePlayerPipController
import su.afk.yummy.tv.feature.player.mobile.view.MobileNativePlayer
import su.afk.yummy.tv.feature.player.mobile.view.MobilePlayerEpisodesList
import su.afk.yummy.tv.feature.player.mobile.view.MobilePlayerBalancerSheet
import su.afk.yummy.tv.feature.player.mobile.view.MobilePlayerDubbingSheet
import su.afk.yummy.tv.feature.player.mobile.view.MobilePlayerMessage
import su.afk.yummy.tv.feature.player.mobile.view.MobilePlayerSkipToast
import su.afk.yummy.tv.feature.player.model.PlayerNextEpisodeUnavailable
import su.afk.yummy.tv.feature.player.navigator.PlayerDestination
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.core.designsystem.R as CoreR

@OptIn(UnstableApi::class)
@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun PlayerMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        PlayerMobileScreen(
            dest = PlayerDestination(
                iframeUrl = "",
                animeTitle = "",
                episode = "",
                playerName = "",
            ),
            state = PlayerState.State(),
            effect = emptyFlow(),
            onEvent = {},
        )
    }

@Composable
fun PlayerMobileScreen(
    dest: PlayerDestination,
    state: PlayerState.State,
    effect: Flow<PlayerState.Effect>,
    onEvent: (PlayerState.Event) -> Unit,

) {
    var showErrorBalancerSheet by rememberSaveable { mutableStateOf(false) }
    var showErrorDubbingSheet by rememberSaveable { mutableStateOf(false) }
    var sourceToastText by remember { mutableStateOf<String?>(null) }
    var nextEpisodeUnavailable by remember { mutableStateOf<PlayerNextEpisodeUnavailable?>(null) }
    LaunchedEffect(effect) {
        effect.collect { eff ->
            when (eff) {
                is PlayerState.Effect.ShowToast -> sourceToastText = eff.message
                is PlayerState.Effect.ShowNextEpisodeUnavailable -> nextEpisodeUnavailable = eff.info
            }
        }
    }
    LaunchedEffect(sourceToastText) {
        if (sourceToastText != null) {
            delay(SOURCE_TOAST_DURATION_MS)
            sourceToastText = null
        }
    }
    LaunchedEffect(dest) {
        onEvent(PlayerState.Event.NavigateToDestination(dest))
    }
    // Сервис плеера поднимается параллельно с извлечением ссылки и переживает смену серии.
    PlayerServiceWarmupEffect()

    val playerNamePrefix = stringResource(R.string.player_name_prefix)
    val uiState = rememberPlayerPlaybackUiState(state, playerNamePrefix)
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    var episodesExpanded by rememberSaveable { mutableStateOf(false) }
    val episodesAvailable = state.streamUrl != null &&
        state.playerError == null &&
        state.kodikBlockedError == null &&
        uiState.episodes.isNotEmpty() &&
        !MobilePlayerPipController.isInPictureInPictureMode
    // Как на YouTube: в портрете видео на весь экран, ручка «Серии» раскрывает список (видео 2/3, список 1/3).
    val episodesBelowPlayer = isPortrait && episodesAvailable && episodesExpanded
    val playerHeightFraction by animateFloatAsState(
        targetValue = if (episodesBelowPlayer) PLAYER_HEIGHT_WITH_EPISODES else 1f,
        label = "playerHeightFraction",
    )

    HideMobilePlayerSystemBars(hide = !episodesBelowPlayer)
    ApplyPlayerOrientation(state.playerOrientationMode)

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
    val streamUrl = state.streamUrl
    val kodikBlockedError = state.kodikBlockedError
    Box(modifier = Modifier.fillMaxSize().testTag("player_screen")) {
        when {
            kodikBlockedError != null -> MobilePlayerMessage(
                heading = uiState.errorBalancerLabel?.let { stringResource(R.string.player_balancer_error_title, it) },
                title = stringResource(R.string.player_quoted_message, kodikBlockedError),
                secondaryActionLabel = if (uiState.canChangePlayer) {
                    stringResource(R.string.player_change_player)
                } else {
                    null
                },
                onSecondaryAction = if (uiState.canChangePlayer) {
                    { showErrorBalancerSheet = true }
                } else {
                    null
                },
                tertiaryActionLabel = if (uiState.canChangeDubbing) {
                    stringResource(R.string.player_change_dubbing)
                } else {
                    null
                },
                onTertiaryAction = if (uiState.canChangeDubbing) {
                    { showErrorDubbingSheet = true }
                } else {
                    null
                },
                onBack = { onEvent(PlayerState.Event.Back) },
            )

            state.playerError != null -> MobilePlayerMessage(
                heading = uiState.errorBalancerLabel?.let { stringResource(R.string.player_balancer_error_title, it) },
                title = state.playerError,
                actionLabel = stringResource(CoreR.string.common_retry),
                onAction = { onEvent(PlayerState.Event.RetryStream) },
                secondaryActionLabel = if (uiState.canChangePlayer) {
                    stringResource(R.string.player_change_player)
                } else {
                    null
                },
                onSecondaryAction = if (uiState.canChangePlayer) {
                    { showErrorBalancerSheet = true }
                } else {
                    null
                },
                tertiaryActionLabel = if (uiState.canChangeDubbing) {
                    stringResource(R.string.player_change_dubbing)
                } else {
                    null
                },
                onTertiaryAction = if (uiState.canChangeDubbing) {
                    { showErrorDubbingSheet = true }
                } else {
                    null
                },
                onBack = { onEvent(PlayerState.Event.Back) },
            )

            streamUrl != null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (episodesBelowPlayer) MaterialTheme.colorScheme.background else Color.Black)
                    .then(if (episodesBelowPlayer) Modifier.statusBarsPadding() else Modifier),
            ) {
                MobileNativePlayer(
                    state = state,
                    ui = uiState,
                    streamUrl = streamUrl,
                    videoTransform = videoTransform,
                    onVideoTransformChanged = { transform ->
                        onEvent(
                            PlayerState.Event.MobileVideoTransformChanged(
                                scale = transform.scale,
                                offsetX = transform.offset.x,
                                offsetY = transform.offset.y,
                            ),
                        )
                    },
                    onEvent = onEvent,
                    nextEpisodeUnavailable = nextEpisodeUnavailable,
                    onNextEpisodeUnavailableShown = { nextEpisodeUnavailable = null },
                    episodesExpanded = episodesExpanded,
                    onEpisodesExpandedChange = { episodesExpanded = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(playerHeightFraction)
                        .then(
                            if (episodesBelowPlayer) {
                                Modifier.consumeWindowInsets(WindowInsets.navigationBars)
                            } else {
                                Modifier
                            },
                        ),
                )
                if (episodesBelowPlayer) {
                    MobilePlayerEpisodesList(
                        episodes = uiState.episodes,
                        currentEpisodeIndex = uiState.currentEpisodeIndex,
                        animeId = state.animeId,
                        posterUrl = state.posterUrl,
                        onEpisodeSelected = {
                            episodesExpanded = false
                            onEvent(PlayerState.Event.EpisodeSelected(it))
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(1f - PLAYER_HEIGHT_WITH_EPISODES),
                    )
                }
            }

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
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        YummyLoadingIndicator(color = Color.White, strokeWidth = 2.dp)
                        Text(stringResource(state.resolveStatus.loadingTextRes()), color = Color.White)
                    }
                    PlayerResolveLogView(state.resolveLog, modifier = Modifier.padding(horizontal = 24.dp))
                    if (state.showChangePlayerHint && (uiState.canChangePlayer || uiState.canChangeDubbing)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (uiState.canChangePlayer) {
                                OutlinedButton(onClick = { showErrorBalancerSheet = true }) {
                                    Text(stringResource(R.string.player_change_player))
                                }
                            }
                            if (uiState.canChangeDubbing) {
                                OutlinedButton(onClick = { showErrorDubbingSheet = true }) {
                                    Text(stringResource(R.string.player_change_dubbing))
                                }
                            }
                        }
                    }
                }
            }
        }
        MobilePlayerSkipToast(
            text = sourceToastText,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
        )
        if (showErrorBalancerSheet && uiState.canChangePlayer) {
            MobilePlayerBalancerSheet(
                balancerNames = uiState.balancerNames,
                balancerAvailability = uiState.balancerAvailability,
                selectedIndex = uiState.currentBalancerIndex,
                balancerExternal = uiState.balancerExternal,
                onBalancerSelected = { index ->
                    showErrorBalancerSheet = false
                    onEvent(PlayerState.Event.BalancerSelected(index, uiState.errorResumePositionMs))
                },
                onDismiss = { showErrorBalancerSheet = false },
            )
        }
        if (showErrorDubbingSheet && uiState.canChangeDubbing) {
            MobilePlayerDubbingSheet(
                playback = uiState,
                onDubbingSelected = { index ->
                    showErrorDubbingSheet = false
                    onEvent(PlayerState.Event.DubbingSelected(index, uiState.errorResumePositionMs))
                },
                onDismiss = { showErrorDubbingSheet = false },
            )
        }
    }
}

private const val SOURCE_TOAST_DURATION_MS = 2_000L

/** Доля высоты видео, когда под ним раскрыт список серий (список — остальная 1/3). */
private const val PLAYER_HEIGHT_WITH_EPISODES = 2f / 3f

@Composable
private fun ApplyPlayerOrientation(mode: PlayerOrientationMode) {
    val context = LocalContext.current
    val activity = remember(context) { MobilePlayerPipController.findActivity(context) }

    DisposableEffect(activity, mode) {
        activity?.requestedOrientation = when (mode) {
            PlayerOrientationMode.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            PlayerOrientationMode.SENSOR_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            PlayerOrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

@Composable
private fun HideMobilePlayerSystemBars(hide: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { MobilePlayerPipController.findActivity(context) }

    DisposableEffect(activity, view, hide) {
        if (!hide) return@DisposableEffect onDispose {}
        val window = activity?.window
            ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val previousBehavior = controller.systemBarsBehavior

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = previousBehavior
        }
    }
}
