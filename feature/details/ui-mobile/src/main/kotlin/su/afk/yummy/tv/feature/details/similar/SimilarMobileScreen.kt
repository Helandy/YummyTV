package su.afk.yummy.tv.feature.details.similar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileContentPosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.feature.details.details.SimilarUiState
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.similar.utils.bestUrl
import su.afk.yummy.tv.feature.details.view.DetailsMobileScaffold

@Composable
fun SimilarMobileScreen(

    state: SimilarState.State,
    effect: Flow<SimilarState.Effect>,
    onEvent: (SimilarState.Event) -> Unit,

) {
    DetailsMobileScaffold(
        title = stringResource(R.string.details_mobile_similar),
        onBack = { onEvent(SimilarState.Event.BackSelected) },
    ) { padding ->
        MobilePosterGrid(contentPadding = padding) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Button(
                    onClick = { onEvent(SimilarState.Event.SourceToggled) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.fromAi) {
                            stringResource(R.string.details_mobile_similar_source_ai)
                        } else {
                            stringResource(R.string.details_mobile_similar_source_site)
                        },
                    )
                }
            }
            when (val similar = state.similarState) {
                SimilarUiState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(R.string.details_mobile_loading))
                }
                SimilarUiState.Empty -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(R.string.details_mobile_similar_empty))
                }
                is SimilarUiState.Content -> {
                    items(similar.items, key = { it.animeId }) { item ->
                        MobileContentPosterCard(
                            title = item.title,
                            posterUrl = item.poster.bestUrl(),
                            rating = item.rating,
                            onClick = { onEvent(SimilarState.Event.AnimeSelected(item.animeId)) },
                        )
                    }
                }
            }
        }
    }
}
