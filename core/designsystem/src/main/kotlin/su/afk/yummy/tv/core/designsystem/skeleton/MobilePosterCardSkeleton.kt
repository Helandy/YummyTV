package su.afk.yummy.tv.core.designsystem.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.dimensions.TITLE_POSTER_ASPECT_RATIO
import su.afk.yummy.tv.core.designsystem.dimensions.currentMobilePosterTextPadding
import su.afk.yummy.tv.core.designsystem.dimensions.currentMobilePosterTitleStyle
import su.afk.yummy.tv.core.designsystem.dimensions.currentMobilePosterWidth

/** Плейсхолдер [su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterCard]: постер и две строки названия. */
@Composable
fun MobilePosterCardSkeleton(
    shimmer: SkeletonShimmer,
    modifier: Modifier = Modifier.width(currentMobilePosterWidth()),
) {
    val titleLineHeight = with(LocalDensity.current) {
        currentMobilePosterTitleStyle().lineHeight.takeIf { it.isSp }?.toDp() ?: 20.dp
    }
    Column(modifier = modifier) {
        SkeletonBlock(
            shimmer = shimmer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(TITLE_POSTER_ASPECT_RATIO),
        )
        // Название в две строки, как у карточки с titleMinLines = 2.
        Column(
            modifier = Modifier.padding(horizontal = currentMobilePosterTextPadding(), vertical = 8.dp),
        ) {
            SkeletonTextLine(shimmer = shimmer, widthFraction = 0.9f, lineHeight = titleLineHeight)
            SkeletonTextLine(shimmer = shimmer, widthFraction = 0.6f, lineHeight = titleLineHeight)
        }
    }
}
