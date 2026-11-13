package su.afk.yummy.tv.core.designsystem.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
    Column(modifier = modifier.width(width)) {
        SkeletonBlock(
            shimmer = shimmer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(TITLE_POSTER_ASPECT_RATIO),
        )
        // Под постером у настоящей карточки: отступ 10dp и название ровно в две строки bodyLarge.
        Column(modifier = Modifier.padding(10.dp)) {
            SkeletonTextLine(shimmer = shimmer, widthFraction = 0.9f, lineHeight = 24.dp)
            SkeletonTextLine(shimmer = shimmer, widthFraction = 0.6f, lineHeight = 24.dp)
        }
    }
}
