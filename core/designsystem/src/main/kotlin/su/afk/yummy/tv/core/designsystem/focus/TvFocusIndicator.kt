package su.afk.yummy.tv.core.designsystem.focus

import su.afk.yummy.tv.core.designsystem.seasonal.seasonalFocusLights
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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

private const val TV_FOCUS_BORDER_ANIMATION_MILLIS = 180

/**
 * Индикатор фокуса для НЕкликабельных фокус-остановок (текст/картинки, по которым
 * нужно проходить пультом при прокрутке). Даёт видимую рамку [focusedBorderColor]
 * (и опционально масштаб), в отличие от голого [Modifier.focusable].
 */
@Composable
fun Modifier.tvFocusIndicator(
    shape: Shape = RoundedCornerShape(8.dp),
    focusedBorderColor: Color = MaterialTheme.colorScheme.primary,
    focusedScale: Float = 1f,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "tvFocusIndicatorScale",
    )
    val borderColor by animateColorAsState(
        targetValue = if (focused) focusedBorderColor else Color.Transparent,
        animationSpec = tween(durationMillis = TV_FOCUS_BORDER_ANIMATION_MILLIS),
        label = "tvFocusIndicatorBorder",
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .seasonalFocusLights(focused, shape)
        .border(
            border = BorderStroke(width = 3.dp, color = borderColor),
            shape = shape,
        )
        .focusable(interactionSource = interactionSource)
}
