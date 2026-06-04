package su.afk.yummy.tv.feature.schedule.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

@Composable
internal fun ScheduleSkeletonBlock(
    modifier: Modifier,
    alpha: Float,
    color: Color,
    shape: RoundedCornerShape,
) {
    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(shape)
            .background(color, shape),
    )
}
