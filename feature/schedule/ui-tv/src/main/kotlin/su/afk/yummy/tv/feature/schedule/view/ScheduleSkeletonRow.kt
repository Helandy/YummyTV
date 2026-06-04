package su.afk.yummy.tv.feature.schedule.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun ScheduleSkeletonRow(alpha: Float, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScheduleSkeletonBlock(
            modifier = Modifier
                .width(74.dp)
                .fillMaxHeight(),
            alpha = alpha,
            color = color,
            shape = RoundedCornerShape(6.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScheduleSkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.58f)
                    .height(22.dp),
                alpha = alpha,
                color = color,
                shape = RoundedCornerShape(5.dp),
            )
            ScheduleSkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(18.dp),
                alpha = alpha,
                color = color,
                shape = RoundedCornerShape(5.dp),
            )
        }
        ScheduleSkeletonBlock(
            modifier = Modifier
                .width(82.dp)
                .height(28.dp),
            alpha = alpha,
            color = color,
            shape = RoundedCornerShape(5.dp),
        )
    }
}
