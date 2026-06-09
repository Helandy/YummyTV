package su.afk.yummy.tv.core.designsystem.presenter.focus

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.tvFocusableClick(
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(8.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onLongClick: (() -> Unit)? = null,
    focusedScale: Float = 1.04f,
    focusedBorderDurationMillis: Int = 150,
): Modifier {
    val focused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "card_scale",
    )
    val borderColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(
            durationMillis = focusedBorderDurationMillis,
            easing = FastOutSlowInEasing
        ),
        label = "card_border",
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .border(
            border = BorderStroke(width = 3.dp, color = borderColor),
            shape = shape,
        )
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick,
        )
}

@Composable
fun TvFocusOverlay(
    focused: Boolean,
    modifier: Modifier = Modifier,
) {
    if (focused) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp),
                ),
        )
    }
}

// ─── Focus Restorer ──────────────────────────────────────────────────────────

@Stable
class FocusRestorerState internal constructor(private val savedIndex: androidx.compose.runtime.MutableIntState) {
    private val focusRequesters = HashMap<Int, FocusRequester>()

    internal fun registerItem(index: Int): FocusRequester =
        focusRequesters.getOrPut(index) { FocusRequester() }

    internal fun onItemFocused(index: Int) {
        savedIndex.intValue = index
    }

    fun restoreFocus() {
        if (focusRequesters.isEmpty()) return
        val target = savedIndex.intValue
        val closest = focusRequesters.entries.minByOrNull { kotlin.math.abs(it.key - target) }
        runCatching { closest?.value?.requestFocus() }
    }
}

@Composable
fun rememberFocusRestorerState(): FocusRestorerState {
    val savedIndex = rememberSaveable { mutableIntStateOf(0) }
    return remember { FocusRestorerState(savedIndex) }
}

fun Modifier.focusRestorerContainer(state: FocusRestorerState): Modifier {
    return this.focusGroup()
}

fun Modifier.focusRestorerItem(index: Int, state: FocusRestorerState): Modifier {
    val requester = state.registerItem(index)
    return this
        .focusRequester(requester)
        .onFocusChanged { if (it.isFocused) state.onItemFocused(index) }
}
