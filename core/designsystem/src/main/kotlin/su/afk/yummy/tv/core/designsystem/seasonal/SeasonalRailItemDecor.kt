package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.skeleton.rememberAnimationsEnabled
import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme

private const val BubbleMillis = 3_000

/**
 * Хэллоуин у пункта боковой рейки: булькающий котёл над пунктом, в свободном просвете.
 * Вне сезона ничего не рисует; касания и фокус не затрагивает.
 */
@Composable
fun Modifier.seasonalRailItemDecor(): Modifier {
    if (rememberSeasonalTheme() != SeasonalTheme.HALLOWEEN) return this
    val bubbles: State<Float> = if (rememberAnimationsEnabled()) {
        rememberInfiniteTransition(label = "railCauldron").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(BubbleMillis, easing = LinearEasing)),
            label = "railCauldronPhase",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    return drawWithContent {
        drawContent()
        drawCauldron(Offset(size.width / 2f, (-2).dp.toPx()), 16.dp.toPx(), bubbles.value)
    }
}
