package su.afk.yummy.tv.core.designsystem.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.dimensions.currentMobilePosterSpacing

private const val MOBILE_ROW_SKELETON_CARDS = 6

/** Плейсхолдер горизонтального ряда постеров с заголовком секции. Карточки за краем обрезаются. */
@Composable
fun MobilePosterRowSkeleton(
    shimmer: SkeletonShimmer,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SkeletonBlock(
            shimmer = shimmer,
            modifier = Modifier
                .padding(contentPadding)
                .width(160.dp)
                .height(20.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(currentMobilePosterSpacing()),
        ) {
            repeat(MOBILE_ROW_SKELETON_CARDS) {
                MobilePosterCardSkeleton(shimmer = shimmer)
            }
        }
    }
}
