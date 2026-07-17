package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.library.model.WatchHistoryEntry
import su.afk.yummy.tv.feature.library.R

@Composable
internal fun LibraryTvHistoryPage(
    history: Flow<PagingData<WatchHistoryEntry>>,
    isSignedIn: Boolean,
    gridFocusRequester: FocusRequester,
    onEntrySelected: (WatchHistoryEntry) -> Unit,
    onDetailsSelected: (WatchHistoryEntry) -> Unit,
) {
    if (!isSignedIn) {
        HistoryMessage(stringResource(R.string.library_history_sign_in))
        return
    }
    val items = history.collectAsLazyPagingItems()
    when {
        items.loadState.refresh is LoadState.Loading -> HistoryMessage(null, true)
        items.loadState.refresh is LoadState.Error -> HistoryMessage(stringResource(R.string.library_history_error))
        items.itemCount == 0 -> HistoryMessage(stringResource(R.string.library_history_empty))
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items.itemCount, key = { index -> items[index]?.videoId ?: index }) { index ->
                items[index]?.let { entry ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (index == 0) Modifier.focusRequester(gridFocusRequester) else Modifier)
                            .clickable { onEntrySelected(entry) },
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AsyncImage(
                                model = entry.screenshotUrl ?: entry.posterUrl,
                                contentDescription = entry.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(90.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(entry.title, style = MaterialTheme.typography.titleLarge)
                                val episode = entry.episode.ifBlank { entry.episodeTitle }
                                if (episode.isNotBlank()) Text(
                                    stringResource(
                                        R.string.library_history_episode,
                                        episode
                                    )
                                )
                                Text(
                                    stringResource(R.string.library_history_details),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { onDetailsSelected(entry) }
                                        .padding(top = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMessage(text: String?, loading: Boolean = false) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (loading) CircularProgressIndicator() else Text(text.orEmpty())
    }
}
