package su.afk.yummy.tv.core.designsystem.presenter.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/** Smooth appearance (fade + scale) for dialog/picker cards shown above a screen. */
@Composable
fun TvOverlayAppear(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val visibleState = remember {
        MutableTransitionState(initialState = false).apply { targetState = true }
    }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(TV_OVERLAY_APPEAR_MILLIS)) +
                scaleIn(
                    initialScale = TV_OVERLAY_APPEAR_INITIAL_SCALE,
                    animationSpec = tween(TV_OVERLAY_APPEAR_MILLIS),
                ),
        exit = ExitTransition.None,
        modifier = modifier,
    ) {
        content()
    }
}

private const val TV_OVERLAY_APPEAR_MILLIS = 200
private const val TV_OVERLAY_APPEAR_INITIAL_SCALE = 0.92f
