package su.afk.yummy.tv.core.designsystem.skeleton

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Прямоугольник скелетона; размер задаёт [modifier]. */
@Composable
fun SkeletonBlock(
    shimmer: SkeletonShimmer,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    Box(modifier = modifier.skeleton(shimmer, shape))
}
