package su.afk.yummy.tv.feature.details.similar.view

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.feature.details.details.SimilarUiState
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.similar.utils.bestUrl

private const val SIMILAR_SKELETON_COUNT = 6

@Composable
internal fun SimilarRecommendationsGrid(
    similarState: SimilarUiState,
    onAnimeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "similar_mobile_loading")
    val alpha by transition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.86f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "similar_mobile_loading_alpha",
    )
    val skeletonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    MobilePosterGrid(
        contentPadding = PaddingValues(bottom = 8.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        when (similarState) {
            SimilarUiState.Loading -> repeat(SIMILAR_SKELETON_COUNT) {
                item {
                    SimilarMobileSkeletonCard(alpha = alpha, color = skeletonColor)
                }
            }

            SimilarUiState.Empty -> item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.details_mobile_similar_empty))
            }

            is SimilarUiState.Content -> {
                items(similarState.items, key = { it.animeId }) { item ->
                    MobilePosterCard(
                        title = item.title,
                        posterUrl = item.poster.bestUrl(),
                        rating = item.rating,
                        posterOverlay = {
                            item.year?.let { year ->
                                Text(
                                    text = year.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.inverseSurface,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .background(
                                            MaterialTheme.colorScheme.inverseOnSurface,
                                            RoundedCornerShape(4.dp),
                                        )
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                )
                            }
                        },
                        onClick = { onAnimeSelected(item.animeId) },
                    )
                }
            }
        }
    }
}
