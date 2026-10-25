package su.afk.yummy.tv.feature.details.mobile.episodes.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.skeleton.SkeletonBlock
import su.afk.yummy.tv.core.designsystem.skeleton.rememberSkeletonShimmer

private const val EPISODE_SKELETON_COUNT = 8

/** Скелетон списка серий на время первой загрузки: карточки с превью и двумя строками текста. */
@Composable
internal fun EpisodesMobileSkeleton(modifier: Modifier = Modifier) {
    val shimmer = rememberSkeletonShimmer()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), enabled = false)
            .navigationBarsPadding()
            .padding(PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp)),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(EPISODE_SKELETON_COUNT) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBlock(
                    shimmer = shimmer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .width(128.dp)
                        .aspectRatio(16f / 9f),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SkeletonBlock(
                        shimmer = shimmer,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(16.dp),
                    )
                    SkeletonBlock(
                        shimmer = shimmer,
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(12.dp),
                    )
                }
            }
        }
    }
}
