package su.afk.yummy.tv.feature.details.mobile.details.view

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.skeleton.SkeletonBlock
import su.afk.yummy.tv.core.designsystem.skeleton.rememberSkeletonShimmer

private const val DESCRIPTION_SKELETON_LINES = 4

/** Скелетон карточки тайтла на время первой загрузки: постер, название, кнопки и описание. */
@Composable
internal fun DetailsMobileSkeleton(modifier: Modifier = Modifier) {
    val shimmer = rememberSkeletonShimmer()
    val posterWidth = (LocalConfiguration.current.screenWidthDp.dp * 0.42f).coerceIn(142.dp, 190.dp)
    val buttonShape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), enabled = false)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonBlock(
            shimmer = shimmer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(posterWidth)
                .aspectRatio(2f / 3f),
        )
        SkeletonBlock(
            shimmer = shimmer,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(24.dp),
        )
        SkeletonBlock(
            shimmer = shimmer,
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .height(16.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SkeletonBlock(
                shimmer = shimmer,
                shape = buttonShape,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            )
            SkeletonBlock(
                shimmer = shimmer,
                shape = buttonShape,
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp),
            )
            SkeletonBlock(
                shimmer = shimmer,
                shape = buttonShape,
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        repeat(DESCRIPTION_SKELETON_LINES) { index ->
            SkeletonBlock(
                shimmer = shimmer,
                modifier = Modifier
                    .fillMaxWidth(if (index == DESCRIPTION_SKELETON_LINES - 1) 0.6f else 1f)
                    .height(14.dp),
            )
        }
    }
}
