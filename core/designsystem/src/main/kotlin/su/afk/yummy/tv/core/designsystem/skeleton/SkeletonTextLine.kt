package su.afk.yummy.tv.core.designsystem.skeleton

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Плейсхолдер одной строки текста: занимает [lineHeight] строки (как реальный текст), а сама плашка
 * ниже и стоит по центру. Так стопка плейсхолдеров совпадает по высоте с настоящими строками.
 */
@Composable
fun SkeletonTextLine(
    shimmer: SkeletonShimmer,
    widthFraction: Float,
    lineHeight: Dp,
    modifier: Modifier = Modifier,
    blockHeight: Dp = (lineHeight - 6.dp).coerceAtLeast(8.dp),
) {
    Box(modifier = modifier.fillMaxWidth().height(lineHeight), contentAlignment = Alignment.CenterStart) {
        SkeletonBlock(
            shimmer = shimmer,
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(blockHeight),
        )
    }
}
