package su.afk.yummy.tv.core.designsystem.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.dimensions.tvPosterGridColumns

private const val TV_GRID_SKELETON_ROWS = 3

/** Плейсхолдер ТВ-грида постеров: те же колонки и отступы, что у грида результатов. */
@Composable
fun TvPosterGridSkeleton(
    modifier: Modifier = Modifier,
    shimmer: SkeletonShimmer = rememberSkeletonShimmer(),
) {
    val cardWidth = currentTvTitleCardDimensions().width
    BoxWithConstraints(modifier = modifier.fillMaxSize().clipToBounds()) {
        val availableWidth = maxWidth - TvScreenPadding.Horizontal - TvScreenPadding.Horizontal
        val columns = tvPosterGridColumns(
            availableWidth = availableWidth,
            cardWidth = cardWidth,
            spacing = TvCardSpacing.Horizontal,
        ).coerceAtLeast(1)
        val cellWidth = (availableWidth - TvCardSpacing.Horizontal * (columns - 1)) / columns
        Column(
            modifier = Modifier
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .padding(start = TvScreenPadding.Horizontal, end = TvScreenPadding.Horizontal, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
        ) {
            repeat(TV_GRID_SKELETON_ROWS) {
                Row(horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal)) {
                    repeat(columns) {
                        TvPosterCardSkeleton(shimmer = shimmer, width = cellWidth)
                    }
                }
            }
        }
    }
}
