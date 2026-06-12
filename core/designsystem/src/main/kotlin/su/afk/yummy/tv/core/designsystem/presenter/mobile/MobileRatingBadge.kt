package su.afk.yummy.tv.core.designsystem.presenter.mobile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import su.afk.yummy.tv.core.designsystem.presenter.components.RatingBadge

@Composable
fun MobileRatingBadge(
    rating: Double,
    modifier: Modifier = Modifier,
) {
    RatingBadge(
        rating = rating,
        modifier = modifier,
    )
}
