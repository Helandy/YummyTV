package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import su.afk.yummy.tv.core.designsystem.components.toRatingTextColor
import su.afk.yummy.tv.feature.details.details.model.ExternalRatingLabel

@Composable
internal fun RatingLabel(rating: ExternalRatingLabel) {
    Text(
        text = rating.label,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        color = rating.rating.toRatingTextColor(),
    )
}
