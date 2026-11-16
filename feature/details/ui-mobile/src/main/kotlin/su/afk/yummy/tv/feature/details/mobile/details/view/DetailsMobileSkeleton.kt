package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.R
import su.afk.yummy.tv.core.designsystem.skeleton.SkeletonBlock
import su.afk.yummy.tv.core.designsystem.skeleton.SkeletonShimmer
import su.afk.yummy.tv.core.designsystem.skeleton.rememberSkeletonShimmer

private const val DESCRIPTION_SKELETON_LINES = 4

/**
 * Скелетон карточки тайтла на время первой загрузки. Размеры повторяют [DetailsMobileHero],
 * [DetailsPrimaryActions], [DetailsSecondaryActions] и [DetailsDescriptionBlock].
 */
@Composable
internal fun DetailsMobileSkeleton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val shimmer = rememberSkeletonShimmer()
    val posterWidth = (LocalConfiguration.current.screenWidthDp.dp * 0.46f).coerceIn(150.dp, 210.dp)
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState(), enabled = false),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Те же отступы, что у DetailsMobileHero: статус-бар + 58dp под кнопки шапки.
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 58.dp, end = 16.dp),
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
                        .width(56.dp)
                        .height(28.dp),
                )
                // Название (headlineMedium, строка 36dp).
                SkeletonBlock(
                    shimmer = shimmer,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(36.dp),
                )
                // Оригинальное название и иконка «Детали».
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SkeletonBlock(
                        shimmer = shimmer,
                        modifier = Modifier
                            .width(150.dp)
                            .height(21.dp),
                    )
                    SkeletonBlock(
                        shimmer = shimmer,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.size(20.dp),
                    )
                }
                // Чипы метаданных.
                SkeletonBlock(
                    shimmer = shimmer,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(22.dp),
                )
                // Прогресс выхода серий (bodyMedium).
                SkeletonBlock(
                    shimmer = shimmer,
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(18.dp),
                )
                // Как DetailsPrimaryActions: зазор 12dp между «Смотреть» и рядом кнопок.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
            // Плитки «Серии» / «Подписки» (≥64dp, с тремя строками текста — около 76dp) и блок под ними.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(2) {
                        SkeletonBlock(
                            shimmer = shimmer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(76.dp),
                        )
                    }
                }
                // Студии и создатели: иконка слева, текст с отступом.
                InfoRowSkeleton(shimmer = shimmer, lines = listOf(0.7f, 0.5f))
                // Жанры: иконка и чипы.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SkeletonBlock(shimmer = shimmer, shape = RoundedCornerShape(4.dp), modifier = Modifier.size(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(70.dp, 96.dp, 80.dp).forEach { chipWidth ->
                            SkeletonBlock(
                                shimmer = shimmer,
                                modifier = Modifier
                                    .width(chipWidth)
                                    .height(24.dp),
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
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
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 12.dp)
                .size(52.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
            )
        }
    }
}

@Composable
private fun InfoRowSkeleton(shimmer: SkeletonShimmer, lines: List<Float>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SkeletonBlock(shimmer = shimmer, shape = RoundedCornerShape(4.dp), modifier = Modifier.size(20.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            lines.forEach { fraction ->
                SkeletonBlock(
                    shimmer = shimmer,
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(16.dp),
                )
            }
        }
    }
}
