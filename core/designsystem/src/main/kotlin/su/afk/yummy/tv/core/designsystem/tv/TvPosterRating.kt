package su.afk.yummy.tv.core.designsystem.tv

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.RatingBadge
import su.afk.yummy.tv.core.designsystem.dimensions.currentTvPosterBadgeScale

/** Рейтинг в правом верхнем углу постера; для слота `posterOverlay` у [TvTitleCard]. */
@Composable
fun BoxScope.TvPosterRating(rating: Double) {
    if (rating <= 0.0) return
    RatingBadge(
        rating = rating,
        scale = currentTvPosterBadgeScale(),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp),
    )
}
