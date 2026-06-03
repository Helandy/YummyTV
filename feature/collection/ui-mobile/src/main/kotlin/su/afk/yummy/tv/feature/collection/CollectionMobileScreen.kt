package su.afk.yummy.tv.feature.collection

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileContentPosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent

@Composable
fun CollectionMobileScreen(
    state: CollectionState.State,
    effect: Flow<CollectionState.Effect>,
    onEvent: (CollectionState.Event) -> Unit,
) {
    MobileStateContent(
        isLoading = state.isLoading,
        error = state.error,
        onRetry = { onEvent(CollectionState.Event.RetrySelected) },
        empty = state.collection?.animes.orEmpty().isEmpty() && !state.isLoading,
    ) {
        val collection = state.collection
        MobilePosterGrid(contentPadding = PaddingValues()) {
            if (collection != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(collection.title)
                }
                items(collection.animes, key = { it.id }) { item ->
                    MobileContentPosterCard(
                        title = item.title,
                        posterUrl = item.posterUrl,
                        rating = item.rating,
                        onClick = { onEvent(CollectionState.Event.AnimeSelected(item.id)) },
                    )
                }
            }
        }
    }
}
