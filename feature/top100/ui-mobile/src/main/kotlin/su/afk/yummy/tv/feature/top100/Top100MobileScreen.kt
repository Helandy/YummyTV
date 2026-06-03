package su.afk.yummy.tv.feature.top100

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileContentPosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.domain.top100.model.AnimeTopType

@Composable
fun Top100MobileScreen(
    state: Top100State.State,
    effect: Flow<Top100State.Effect>,
    onEvent: (Top100State.Event) -> Unit,
) {
    MobilePosterGrid(contentPadding = PaddingValues()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimeTopType.entries.forEach { type ->
                    FilterChip(
                        selected = type == state.selectedType,
                        onClick = { onEvent(Top100State.Event.TypeSelected(type)) },
                        label = { Text(type.name) },
                    )
                }
            }
        }
        if (state.error != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Button(onClick = { onEvent(Top100State.Event.RetrySelected) }) {
                    Text("Повторить: ${state.error}")
                }
            }
        }
        if (state.isLoading && state.items.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { Text("Загрузка...") }
        }
        items(state.items, key = { it.id }) { item ->
            MobileContentPosterCard(
                title = item.title,
                posterUrl = item.posterUrl,
                rating = item.rating,
                onClick = { onEvent(Top100State.Event.AnimeSelected(item.id)) },
            )
        }
        if (state.canLoadMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Button(
                    onClick = { onEvent(Top100State.Event.LoadMore) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.isLoadingMore) "Загрузка..." else "Показать ещё")
                }
            }
        }
    }
}
