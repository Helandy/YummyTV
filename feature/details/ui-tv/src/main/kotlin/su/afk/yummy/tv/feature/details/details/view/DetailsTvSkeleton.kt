package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.skeleton.SkeletonBlock
import su.afk.yummy.tv.core.designsystem.skeleton.rememberSkeletonShimmer

private const val TV_DESCRIPTION_SKELETON_LINES = 3
private const val TV_BUTTON_SKELETON_ROWS = 2

/** Скелетон героя деталки на ТВ: колонка с текстом и кнопками слева, постер справа. */
@Composable
internal fun DetailsTvSkeleton(modifier: Modifier = Modifier) {
    val shimmer = rememberSkeletonShimmer()
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SkeletonBlock(
                shimmer = shimmer,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(40.dp),
            )
            SkeletonBlock(
                shimmer = shimmer,
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(18.dp),
            )
            SkeletonBlock(
                shimmer = shimmer,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(18.dp),
            )
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(TV_BUTTON_SKELETON_ROWS) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        repeat(2) {
                            SkeletonBlock(
                                shimmer = shimmer,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                            )
                        }
                    }
                }
            }
            SkeletonBlock(
                shimmer = shimmer,
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(28.dp),
            )
            repeat(TV_DESCRIPTION_SKELETON_LINES) { index ->
                SkeletonBlock(
                    shimmer = shimmer,
                    modifier = Modifier
                        .fillMaxWidth(if (index == TV_DESCRIPTION_SKELETON_LINES - 1) 0.7f else 1f)
                        .height(16.dp),
                )
            }
        }
        SkeletonBlock(
            shimmer = shimmer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(340.dp)
                .aspectRatio(2f / 3f),
        )
    }
}
