package su.afk.yummy.tv.core.designsystem.skeleton

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private val DefaultSkeletonShape = RoundedCornerShape(8.dp)

/** Заливает элемент плейсхолдером скелетона с бегущим бликом [shimmer]. */
fun Modifier.skeleton(
    shimmer: SkeletonShimmer,
    shape: Shape = DefaultSkeletonShape,
): Modifier = clip(shape).drawBehind {
    drawRect(shimmer.baseColor)
    val progress = shimmer.progress?.value ?: return@drawBehind
    val band = size.width.coerceAtLeast(size.height) * 0.6f
    val center = -band + (size.width + band * 2) * progress
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(shimmer.baseColor, shimmer.highlightColor, shimmer.baseColor),
            start = Offset(center - band, 0f),
            end = Offset(center + band, size.height),
        ),
    )
}
