package su.afk.yummy.tv.feature.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileContentPosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.feature.library.mobile.R
import su.afk.yummy.tv.feature.library.utils.mobileTitle

@Composable
fun LibraryMobileScreen(

    state: LibraryState.State,
    effect: Flow<LibraryState.Effect>,
    onEvent: (LibraryState.Event) -> Unit,

) {
    MobilePosterGrid(contentPadding = PaddingValues()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LibraryTab.entries.forEach { tab ->
                    AssistChip(
                        onClick = { onEvent(LibraryState.Event.TabSelected(tab)) },
                        label = { Text(tab.mobileTitle()) },
                    )
                }
            }
        }
        when (state.selectedTab) {
            LibraryTab.CONTINUE_WATCHING -> {
                items(state.continueWatching, key = { "${it.animeId}-${it.episode}" }) { entry ->
                    MobilePosterCard(
                        title = entry.animeTitle.ifBlank { stringResource(R.string.library_mobile_episode, entry.episode) },
                        posterUrl = entry.posterUrl.ifBlank { null },
                        subtitle = stringResource(R.string.library_mobile_episode, entry.episode),
                        onClick = { onEvent(LibraryState.Event.ContinueWatchingSelected(entry)) },
                    )
                }
            }
            LibraryTab.FAVORITES -> {
                items(state.items.filter { it.isFavorite }, key = { it.animeId }) { item ->
                    MobileContentPosterCard(
                        title = item.title,
                        posterUrl = item.posterMegaUrl ?: item.posterFullsizeUrl ?: item.posterBigUrl ?: item.posterMediumUrl ?: item.posterSmallUrl,
                        onClick = { onEvent(LibraryState.Event.AnimeSelected(item.animeId)) },
                    )
                }
            }
            else -> {
                val remote = state.remoteItems[state.selectedTab].orEmpty()
                items(remote, key = { it.animeId }) { item ->
                    MobileContentPosterCard(
                        title = item.title,
                        posterUrl = item.poster?.mega ?: item.poster?.fullsize ?: item.posterUrl,
                        rating = item.rating,
                        onClick = { onEvent(LibraryState.Event.RemoteAnimeSelected(item.animeId)) },
                    )
                }
            }
        }
        if (state.remoteError != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Button(onClick = { onEvent(LibraryState.Event.ScreenResumed) }) {
                    Text(stringResource(R.string.library_mobile_retry_error, state.remoteError.orEmpty()))
                }
            }
        }
    }
}
