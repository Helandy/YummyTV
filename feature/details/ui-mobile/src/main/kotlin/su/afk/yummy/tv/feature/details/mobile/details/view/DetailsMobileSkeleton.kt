package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

/**
 * Скелетон карточки тайтла на время первой загрузки. Размеры повторяют [DetailsMobileHero],
 * [DetailsPrimaryActions], [DetailsSecondaryActions] и [DetailsDescriptionBlock].
 */
@Composable
internal fun DetailsMobileSkeleton(modifier: Modifier = Modifier) {
    val shimmer = rememberSkeletonShimmer()
    val posterWidth = (LocalConfiguration.current.screenWidthDp.dp * 0.46f).coerceIn(150.dp, 210.dp)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), enabled = false),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkeletonBlock(
                shimmer = shimmer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .width(posterWidth)
                    .aspectRatio(2f / 3f),
            )
            // Строка рейтингов.
            SkeletonBlock(
                shimmer = shimmer,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(160.dp)
                    .height(24.dp),
            )
            // Название (headlineMedium).
            SkeletonBlock(
                shimmer = shimmer,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(32.dp),
            )
            // Оригинальное название.
            SkeletonBlock(
                shimmer = shimmer,
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(20.dp),
            )
            // Чипы метаданных.
            SkeletonBlock(
                shimmer = shimmer,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(24.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkeletonBlock(
                    shimmer = shimmer,
                    shape = RoundedCornerShape(25.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(2) {
                        SkeletonBlock(
                            shimmer = shimmer,
                            shape = RoundedCornerShape(23.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                        )
                    }
                }
            }
        }
        // Карточки «Озвучки» / «Серии».
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(2) {
                SkeletonBlock(
                    shimmer = shimmer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(DESCRIPTION_SKELETON_LINES) { index ->
                SkeletonBlock(
                    shimmer = shimmer,
                    modifier = Modifier
                        .fillMaxWidth(if (index == DESCRIPTION_SKELETON_LINES - 1) 0.6f else 1f)
                        .height(16.dp),
                )
            }
        }
    }
}
