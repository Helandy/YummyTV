package su.afk.yummy.tv.feature.reviews.mobile.view

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.reviews.model.ReviewStatus
import su.afk.yummy.tv.feature.reviews.mobile.utils.reviewStatusColor
import su.afk.yummy.tv.feature.reviews.mobile.utils.reviewStatusLabel

@Composable
internal fun ReviewStatusBadge(status: ReviewStatus) {
    val color = status.reviewStatusColor()
    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = status.reviewStatusLabel(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}
