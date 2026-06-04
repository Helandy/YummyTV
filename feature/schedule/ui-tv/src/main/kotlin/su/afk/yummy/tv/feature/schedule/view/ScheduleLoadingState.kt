package su.afk.yummy.tv.feature.schedule.view

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding

@Composable
internal fun ScheduleLoadingState() {
    val transition = rememberInfiniteTransition(label = "schedule_loading")
    val alpha by transition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.86f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "schedule_loading_alpha",
    )
    val baseColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val brightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = TvScreenPadding.Horizontal,
                end = TvScreenPadding.Horizontal,
                top = 18.dp,
                bottom = TvScreenPadding.Vertical,
            ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            repeat(7) { index ->
                ScheduleSkeletonBlock(
                    modifier = Modifier
                        .width(78.dp)
                        .height(72.dp),
                    alpha = alpha,
                    color = if (index == 0) brightColor else baseColor,
                    shape = RoundedCornerShape(10.dp),
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(6) {
                ScheduleSkeletonRow(alpha = alpha, color = baseColor)
            }
        }
    }
}
