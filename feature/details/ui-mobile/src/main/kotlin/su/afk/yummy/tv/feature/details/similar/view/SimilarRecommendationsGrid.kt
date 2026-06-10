package su.afk.yummy.tv.feature.details.similar.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.feature.details.details.SimilarUiState
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.similar.utils.bestUrl

@Composable
internal fun SimilarRecommendationsGrid(
    similarState: SimilarUiState,
    onAnimeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    MobilePosterGrid(
        contentPadding = PaddingValues(bottom = 8.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        when (similarState) {
            SimilarUiState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.details_mobile_loading))
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
                        onClick = { onAnimeSelected(item.animeId) },
                    )
                }
            }
        }
    }
}
