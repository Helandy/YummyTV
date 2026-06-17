package su.afk.yummy.tv.core.designsystem.presenter.focus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    focusedBorderColor: Color = MaterialTheme.colorScheme.primary,
): Modifier {
    val focused by interactionSource.collectIsFocusedAsState()
    val scale = if (focused) focusedScale else 1f
    val borderColor = if (focused) focusedBorderColor else Color.Transparent

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
