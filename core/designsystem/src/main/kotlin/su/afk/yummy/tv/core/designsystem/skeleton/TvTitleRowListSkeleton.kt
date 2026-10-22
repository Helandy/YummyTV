package su.afk.yummy.tv.core.designsystem.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.dimensions.TITLE_POSTER_ASPECT_RATIO
import su.afk.yummy.tv.core.designsystem.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.dimensions.TvScreenPadding

private const val TV_ROW_SKELETON_COUNT = 4
private val PosterWidth = 110.dp

/** Плейсхолдер ТВ-списка построчных карточек тайтлов. */
@Composable
fun TvTitleRowListSkeleton(
    modifier: Modifier = Modifier,
    shimmer: SkeletonShimmer = rememberSkeletonShimmer(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .padding(start = TvScreenPadding.Horizontal, end = TvScreenPadding.Horizontal, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
    ) {
        repeat(TV_ROW_SKELETON_COUNT) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SkeletonBlock(
                    shimmer = shimmer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.width(PosterWidth).height(PosterWidth / TITLE_POSTER_ASPECT_RATIO),
                )
                Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SkeletonBlock(shimmer = shimmer, modifier = Modifier.fillMaxWidth(0.5f).height(16.dp))
                    SkeletonBlock(shimmer = shimmer, modifier = Modifier.fillMaxWidth(0.2f).height(14.dp))
                    SkeletonBlock(shimmer = shimmer, modifier = Modifier.fillMaxWidth(0.9f).height(14.dp))
                    SkeletonBlock(shimmer = shimmer, modifier = Modifier.fillMaxWidth(0.7f).height(14.dp))
                }
            }
        }
    }
}
