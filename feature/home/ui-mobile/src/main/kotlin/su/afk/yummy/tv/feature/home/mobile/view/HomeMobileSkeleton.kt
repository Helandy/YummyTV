package su.afk.yummy.tv.feature.home.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.skeleton.MobilePosterRowSkeleton
import su.afk.yummy.tv.core.designsystem.skeleton.SkeletonBlock
import su.afk.yummy.tv.core.designsystem.skeleton.rememberSkeletonShimmer

private const val HOME_SKELETON_ROWS = 3

/** Скелетон ленты Главной на время первой загрузки: баннер сезона и несколько рядов постеров. */
@Composable
internal fun HomeMobileSkeleton(modifier: Modifier = Modifier) {
    val shimmer = rememberSkeletonShimmer()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), enabled = false)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SkeletonBlock(
            shimmer = shimmer,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        )
        repeat(HOME_SKELETON_ROWS) {
            MobilePosterRowSkeleton(shimmer = shimmer)
        }
    }
}
