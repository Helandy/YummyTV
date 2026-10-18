package su.afk.yummy.tv.core.designsystem.mobile.cards

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import su.afk.yummy.tv.core.designsystem.components.RatingBadge

@Composable
fun MobileRatingBadge(
    rating: Double,
    modifier: Modifier = Modifier,
    showStar: Boolean = true,
) {
    RatingBadge(
        rating = rating,
        modifier = modifier,
        showStar = showStar,
    )
}
