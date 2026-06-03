package su.afk.yummy.tv.feature.details.collections

import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.feature.details.view.DetailsMobileScaffold

@Composable
fun CollectionsMobileScreen(
    state: CollectionsState.State,
    effect: Flow<CollectionsState.Effect>,
    onEvent: (CollectionsState.Event) -> Unit,
) {
    DetailsMobileScaffold(title = "Коллекции", onBack = { onEvent(CollectionsState.Event.BackSelected) }) { padding ->
        MobileStateContent(
            isLoading = state.isLoading,
            error = state.error,
            onRetry = { onEvent(CollectionsState.Event.RetrySelected) },
            empty = state.collections.isEmpty() && !state.isLoading,
        ) {
            MobilePosterGrid(contentPadding = padding) {
                items(state.collections, key = { it.id }) { item ->
                    MobilePosterCard(
                        title = item.title,
                        posterUrl = item.poster?.mega ?: item.poster?.fullsize ?: item.posterUrl,
                        subtitle = item.views?.let { "$it просмотров" },
                        onClick = { onEvent(CollectionsState.Event.CollectionSelected(item.id)) },
                    )
                }
            }
        }
    }
}
