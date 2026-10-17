package su.afk.yummy.tv.feature.home.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.skeleton.TvPosterRowSkeleton
import su.afk.yummy.tv.core.designsystem.skeleton.rememberSkeletonShimmer

private const val HOME_TV_SKELETON_ROWS = 3

/**
 * Скелетон дашборда Главной на время первой загрузки. Сам по себе не фокусируется — фокус
 * держит [modifier] от `tvFocusablePlaceholder`, как у прежнего лоадера.
 */
@Composable
internal fun HomeTvSkeleton(modifier: Modifier = Modifier) {
    val shimmer = rememberSkeletonShimmer()
    Column(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .wrapContentHeight(align = Alignment.Top, unbounded = true)
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        repeat(HOME_TV_SKELETON_ROWS) {
            TvPosterRowSkeleton(shimmer = shimmer)
        }
    }
}
