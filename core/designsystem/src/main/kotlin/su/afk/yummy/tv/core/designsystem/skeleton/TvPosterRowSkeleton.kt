package su.afk.yummy.tv.core.designsystem.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import su.afk.yummy.tv.core.designsystem.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.dimensions.TvScreenPadding

private const val TV_ROW_SKELETON_CARDS = 8

/** Плейсхолдер ТВ-ряда тайтлов с заголовком секции. Карточки за краем экрана обрезаются. */
@Composable
fun TvPosterRowSkeleton(
    shimmer: SkeletonShimmer,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonBlock(
            shimmer = shimmer,
            modifier = Modifier
                .padding(horizontal = TvScreenPadding.Horizontal)
                .width(220.dp)
                .height(24.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .padding(horizontal = TvScreenPadding.Horizontal),
            horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal),
        ) {
            repeat(TV_ROW_SKELETON_CARDS) {
                TvPosterCardSkeleton(shimmer = shimmer)
            }
        }
    }
}
