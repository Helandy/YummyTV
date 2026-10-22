package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.YummyLoadingIndicator
import su.afk.yummy.tv.core.designsystem.skeleton.rememberAnimationsEnabled
import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme

private const val BubbleMillis = 1_600

/** Большой индикатор загрузки на Хэллоуин: ведьмин котёл, из которого поднимаются пузыри. */
@Composable
fun CauldronLoader(modifier: Modifier = Modifier) {
    val bubbles: State<Float> = if (rememberAnimationsEnabled()) {
        rememberInfiniteTransition(label = "cauldron").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(BubbleMillis, easing = LinearEasing)),
            label = "cauldronBubbles",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    Canvas(modifier.defaultMinSize(72.dp, 72.dp)) {
        drawCauldron(Offset(size.width / 2f, size.height * 0.95f), size.minDimension * 0.6f, bubbles.value)
    }
}

/** Индикатор полноэкранной загрузки: котёл на Хэллоуин, иначе обычный [YummyLoadingIndicator]. */
@Composable
fun SeasonalFullscreenLoader(modifier: Modifier = Modifier) {
    if (rememberSeasonalTheme() == SeasonalTheme.HALLOWEEN) {
        CauldronLoader(Modifier.size(88.dp))
    } else {
        YummyLoadingIndicator(modifier)
    }
}
