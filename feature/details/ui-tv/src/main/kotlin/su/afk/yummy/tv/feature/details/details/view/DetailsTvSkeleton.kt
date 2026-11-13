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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.skeleton.SkeletonBlock
import su.afk.yummy.tv.core.designsystem.skeleton.rememberSkeletonShimmer

private const val TV_DESCRIPTION_SKELETON_LINES = 3
private const val TV_LABELED_BUTTON_SKELETON_COUNT = 2
private const val TV_ICON_BUTTON_SKELETON_COUNT = 4

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
                    .height(44.dp),
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
            // Главные кнопки — две пилюли 1.5 : 1 и круг «Любимого»; под ними пилюли «Серии»/«Подписки» и иконки.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf(1.5f, 1f).forEach { weight ->
                    SkeletonBlock(
                        shimmer = shimmer,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(weight)
                            .height(52.dp),
                    )
                }
                SkeletonBlock(
                    shimmer = shimmer,
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp),
                )
            }
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                repeat(TV_LABELED_BUTTON_SKELETON_COUNT) {
                    SkeletonBlock(
                        shimmer = shimmer,
                        shape = CircleShape,
                        modifier = Modifier
                            .width(120.dp)
                            .height(48.dp),
                    )
                }
                repeat(TV_ICON_BUTTON_SKELETON_COUNT) {
                    SkeletonBlock(
                        shimmer = shimmer,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp),
                    )
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
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .width(340.dp)
                .aspectRatio(2f / 3f),
        )
    }
}
