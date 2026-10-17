package su.afk.yummy.tv.core.designsystem.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.dimensions.TITLE_POSTER_ASPECT_RATIO
import su.afk.yummy.tv.core.designsystem.dimensions.currentTvTitleCardDimensions

/** Плейсхолдер ТВ-карточки тайтла тех же размеров, что [su.afk.yummy.tv.core.designsystem.tv.TvTitleCard]. */
@Composable
fun TvPosterCardSkeleton(
    shimmer: SkeletonShimmer,
    modifier: Modifier = Modifier,
    width: Dp = currentTvTitleCardDimensions().width,
) {
    Column(
        modifier = modifier.width(width),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SkeletonBlock(
            shimmer = shimmer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(TITLE_POSTER_ASPECT_RATIO),
        )
        SkeletonBlock(
            shimmer = shimmer,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(14.dp),
        )
    }
}
