package su.afk.yummy.tv.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileContentPosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.feature.search.mobile.R

@Composable
fun SearchMobileScreen(
    state: SearchState.State,
    effect: Flow<SearchState.Effect>,
    onEvent: (SearchState.Event) -> Unit,
) {
    MobilePosterGrid(contentPadding = PaddingValues(0.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onEvent(SearchState.Event.QueryChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_mobile_query)) },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { onEvent(SearchState.Event.OpenFilters) }) {
                        Text(stringResource(R.string.search_mobile_filters_short))
                    }
                },
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Button(
                onClick = { onEvent(SearchState.Event.SearchSubmitted) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.search_mobile_submit))
            }
        }
        val error = state.error
        if (error != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
        if (state.isLoading && state.items.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.search_mobile_loading), style = MaterialTheme.typography.bodyMedium)
            }
        }
        items(state.items, key = { it.id }) { item ->
            MobileContentPosterCard(
                title = item.title,
                posterUrl = item.posterUrl,
                rating = item.rating,
                onClick = { onEvent(SearchState.Event.ItemSelected(item.id)) },
            )
        }
        if (state.canLoadMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Button(
                    onClick = { onEvent(SearchState.Event.LoadMore) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.isLoading) {
                            stringResource(R.string.search_mobile_loading)
                        } else {
                            stringResource(R.string.search_mobile_load_more)
                        },
                    )
                }
            }
        }
        if (state.isFilterPanelOpen) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = { onEvent(SearchState.Event.ApplyFilters) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.search_mobile_apply))
                    }
                    Button(onClick = { onEvent(SearchState.Event.ResetFilters) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.search_mobile_reset))
                    }
                }
            }
        }
    }
}
