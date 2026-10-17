package su.afk.yummy.tv.core.designsystem.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.dimensions.TITLE_POSTER_ASPECT_RATIO
import su.afk.yummy.tv.core.designsystem.dimensions.currentMobilePosterWidth

/** Плейсхолдер [su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterCard]: постер и две строки названия. */
@Composable
fun MobilePosterCardSkeleton(
    shimmer: SkeletonShimmer,
    modifier: Modifier = Modifier.width(currentMobilePosterWidth()),
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SkeletonBlock(
            shimmer = shimmer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(TITLE_POSTER_ASPECT_RATIO),
        )
        SkeletonBlock(
            shimmer = shimmer,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(12.dp),
        )
        SkeletonBlock(
            shimmer = shimmer,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(12.dp),
        )
    }
}
