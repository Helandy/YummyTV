package su.afk.yummy.tv.feature.details.episodes.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.skeleton.SkeletonBlock
import su.afk.yummy.tv.core.designsystem.skeleton.rememberSkeletonShimmer

private const val EPISODE_SKELETON_COUNT = 18
private val SkeletonCardMinWidth = 220.dp
private val SkeletonThumbnailHeight = 124.dp

/** Скелетон сетки серий на ТВ: та же геометрия, что у [EpisodesGrid], без фокусируемых элементов. */
@Composable
internal fun EpisodesTvSkeleton(modifier: Modifier = Modifier) {
    val shimmer = rememberSkeletonShimmer()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = SkeletonCardMinWidth),
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = false,
        contentPadding = PaddingValues(
            horizontal = TvScreenPadding.Horizontal,
            vertical = TvScreenPadding.Vertical,
        ),
        horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal),
        verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            SkeletonBlock(
                shimmer = shimmer,
                modifier = Modifier
                    .width(220.dp)
                    .height(28.dp),
            )
        }
        items(List(EPISODE_SKELETON_COUNT) { it }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBlock(
                    shimmer = shimmer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SkeletonThumbnailHeight),
                )
                SkeletonBlock(
                    shimmer = shimmer,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp),
                )
            }
        }
    }
}
