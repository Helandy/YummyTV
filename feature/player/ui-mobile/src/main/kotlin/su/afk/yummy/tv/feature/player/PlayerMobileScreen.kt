package su.afk.yummy.tv.feature.player

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.feature.player.model.MobileVideoTransform
import su.afk.yummy.tv.feature.player.model.MobileVideoTransformKey
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.view.MobileNativePlayer
import su.afk.yummy.tv.feature.player.view.PlayerMessage

@OptIn(UnstableApi::class)
@Composable
fun PlayerMobileScreen(
    state: PlayerState.State,
    effect: Flow<PlayerState.Effect>,
    onEvent: (PlayerState.Event) -> Unit,

) {
    BackHandler { onEvent(PlayerState.Event.Back) }

    val videoTransforms =
        remember { mutableStateMapOf<MobileVideoTransformKey, MobileVideoTransform>() }
    val videoTransformKey = remember(state.animeId, state.balancerIndex) {
        MobileVideoTransformKey(
            animeId = state.animeId,
            balancerIndex = state.balancerIndex,
        )
    }
    val videoTransform = videoTransforms[videoTransformKey] ?: MobileVideoTransform.Default
    val streamUrl = state.streamUrl
    when {
        streamUrl != null -> MobileNativePlayer(
            state = state,
            streamUrl = streamUrl,
            videoTransform = videoTransform,
            onVideoTransformChanged = { transform ->
                videoTransforms[videoTransformKey] = transform
            },
            onEvent = onEvent,
        )
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
