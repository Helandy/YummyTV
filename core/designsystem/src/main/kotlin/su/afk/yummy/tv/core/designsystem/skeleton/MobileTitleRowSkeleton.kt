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
        // Как у карточки: отступ 8dp, шаг 4dp, название (titleSmall), рейтинг с годом и до 4 строк описания.
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SkeletonTextLine(shimmer = shimmer, widthFraction = 0.8f, lineHeight = 20.dp)
            SkeletonTextLine(shimmer = shimmer, widthFraction = 0.4f, lineHeight = 20.dp)
            SkeletonTextLine(shimmer = shimmer, widthFraction = 0.95f, lineHeight = 16.dp)
            SkeletonTextLine(shimmer = shimmer, widthFraction = 0.9f, lineHeight = 16.dp)
            SkeletonTextLine(shimmer = shimmer, widthFraction = 0.95f, lineHeight = 16.dp)
            SkeletonTextLine(shimmer = shimmer, widthFraction = 0.7f, lineHeight = 16.dp)
        }
    }
}
