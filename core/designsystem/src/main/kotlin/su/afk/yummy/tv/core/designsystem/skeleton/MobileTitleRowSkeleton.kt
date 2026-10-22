package su.afk.yummy.tv.core.designsystem.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.dimensions.TITLE_POSTER_ASPECT_RATIO

private val PosterWidth = 96.dp

/** Плейсхолдер [su.afk.yummy.tv.core.designsystem.mobile.cards.MobileTitleRowCard]. */
@Composable
fun MobileTitleRowSkeleton(
    shimmer: SkeletonShimmer,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonBlock(
            shimmer = shimmer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.width(PosterWidth).height(PosterWidth / TITLE_POSTER_ASPECT_RATIO),
        )
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBlock(shimmer = shimmer, modifier = Modifier.fillMaxWidth(0.8f).height(14.dp))
            SkeletonBlock(shimmer = shimmer, modifier = Modifier.fillMaxWidth(0.4f).height(12.dp))
            SkeletonBlock(shimmer = shimmer, modifier = Modifier.fillMaxWidth(0.95f).height(12.dp))
            SkeletonBlock(shimmer = shimmer, modifier = Modifier.fillMaxWidth(0.7f).height(12.dp))
        }
    }
}
