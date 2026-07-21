package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

internal const val TV_PLAYER_FOCUS_ANIMATION_DURATION_MS = 150

private const val TV_PLAYER_FOCUSED_SCALE = 1.05f

@Composable
internal fun Modifier.tvPlayerFocusScale(focused: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (focused) TV_PLAYER_FOCUSED_SCALE else 1f,
        animationSpec = tween(TV_PLAYER_FOCUS_ANIMATION_DURATION_MS),
        label = "tvPlayerFocusScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
