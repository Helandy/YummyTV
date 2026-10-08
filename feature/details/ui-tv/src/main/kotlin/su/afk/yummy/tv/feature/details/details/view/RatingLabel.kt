package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import su.afk.yummy.tv.core.designsystem.components.toRatingTextColor
import su.afk.yummy.tv.feature.details.details.model.ExternalRatingLabel

@Composable
internal fun RatingLabel(rating: ExternalRatingLabel) {
    Text(
        text = rating.label,
        fontSize = 14.sp,
        maxLines = 1,
        fontWeight = FontWeight.Bold,
        color = rating.rating.toRatingTextColor(),
    )
}
