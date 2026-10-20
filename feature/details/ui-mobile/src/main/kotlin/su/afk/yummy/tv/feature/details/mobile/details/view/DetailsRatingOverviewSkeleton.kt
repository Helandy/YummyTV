package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.MobileSectionHeader
import su.afk.yummy.tv.core.designsystem.skeleton.SkeletonBlock
import su.afk.yummy.tv.core.designsystem.skeleton.rememberSkeletonShimmer
import su.afk.yummy.tv.feature.details.presentation.R

/** Скелетон карточки рейтинга, пока сводка и статистика списков грузятся. */
@Composable
internal fun DetailsRatingOverviewSkeleton(modifier: Modifier = Modifier) {
    val shimmer = rememberSkeletonShimmer()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MobileSectionHeader(title = stringResource(R.string.details_rating_button))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("details_rating_skeleton"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SkeletonBlock(shimmer = shimmer, modifier = Modifier.width(110.dp).height(40.dp))
                        SkeletonBlock(shimmer = shimmer, modifier = Modifier.width(80.dp).height(14.dp))
                    }
                    SkeletonBlock(
                        shimmer = shimmer,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    )
                }
                SkeletonBlock(shimmer = shimmer, modifier = Modifier.fillMaxWidth(0.4f).height(18.dp))
                SkeletonBlock(
                    shimmer = shimmer,
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                )
                SkeletonBlock(shimmer = shimmer, modifier = Modifier.fillMaxWidth(0.8f).height(16.dp))
            }
        }
    }
}
