package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.preferences.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.feature.library.LibraryState
import su.afk.yummy.tv.feature.library.LibraryTab
import su.afk.yummy.tv.feature.library.mobile.R
import su.afk.yummy.tv.feature.library.model.PendingLibraryMobileRemoval
import su.afk.yummy.tv.feature.library.utils.mobileTabItemCount
import su.afk.yummy.tv.feature.library.utils.mobileTitle
import su.afk.yummy.tv.feature.library.utils.posterUrl
import su.afk.yummy.tv.feature.library.utils.shouldShowRemoteLoader
import su.afk.yummy.tv.feature.library.utils.userAnimeList

@Composable
internal fun LibraryMobilePage(
    tab: LibraryTab,
    state: LibraryState.State,
    onEvent: (LibraryState.Event) -> Unit,
    onRemovalRequested: (PendingLibraryMobileRemoval) -> Unit,
) {
    val continueWatchingTitle = LibraryTab.CONTINUE_WATCHING.mobileTitle()
    val favoritesTitle = LibraryTab.FAVORITES.mobileTitle()
    val selectedTabTitle = tab.mobileTitle()
    val showRemoteLoader = state.shouldShowRemoteLoader(tab)
    val isEmpty = state.mobileTabItemCount(tab) == 0

    MobilePosterGrid(contentPadding = PaddingValues()) {
        if (showRemoteLoader) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LibraryMobileLoadingIndicator()
            }
        }

        if (isEmpty && !showRemoteLoader) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LibraryMobileEmptyList()
            }
        }

        when (tab) {
            LibraryTab.CONTINUE_WATCHING -> {
                state.continueWatching.forEach { entry ->
                    item(
                        key = "${entry.animeId}-${entry.episode}",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        val episodeLabel = stringResource(
                            R.string.library_mobile_episode,
                            entry.episode.ifBlank { "?" },
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            LibraryMobileContinueWatchingCard(
                                entry = entry,
                                episodeLabel = episodeLabel,
                                modifier = Modifier.continueWatchingCardWidth(
                                    state.continueWatchingCardSize,
                                ),
                                onDetails = {
                                    onEvent(
                                        LibraryState.Event.ContinueWatchingDetailsSelected(entry),
                                    )
                                },
                                onDelete = {
                                    onRemovalRequested(
                                        PendingLibraryMobileRemoval.WatchProgress(
                                            entry = entry,
                                            listTitle = continueWatchingTitle,
                                        ),
                                    )
                                },
                                onClick = {
                                    onEvent(LibraryState.Event.ContinueWatchingSelected(entry))
                                },
                            )
                        }
                    }
                }
            }

            LibraryTab.FAVORITES -> {
                items(state.items.filter { it.isFavorite }, key = { it.animeId }) { item ->
                    MobilePosterCard(
                        title = item.title,
                        posterUrl = item.posterUrl(),
                        posterOverlay = {
                            LibraryMobileDeleteButton(
                                onClick = {
                                    onRemovalRequested(
                                        PendingLibraryMobileRemoval.Favorite(
                                            animeId = item.animeId,
                                            title = item.title,
                                            listTitle = favoritesTitle,
                                        ),
                                    )
                                },
                            )
                        },
                        onClick = { onEvent(LibraryState.Event.AnimeSelected(item.animeId)) },
                    )
                }
            }

            else -> {
                val localList = tab.userAnimeList()
                val localItems = state.items.filter { it.listId == localList?.id }
                items(localItems, key = { it.animeId }) { item ->
                    MobilePosterCard(
                        title = item.title,
                        posterUrl = item.posterUrl(),
                        posterOverlay = {
                            LibraryMobileDeleteButton(
                                onClick = {
                                    onRemovalRequested(
                                        PendingLibraryMobileRemoval.ListEntry(
                                            animeId = item.animeId,
                                            title = item.title,
                                            listTitle = selectedTabTitle,
                                        ),
                                    )
                                },
                            )
                        },
                        onClick = { onEvent(LibraryState.Event.AnimeSelected(item.animeId)) },
                    )
                }
            }
        }

        if (tab == state.selectedTab && state.remoteError != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LibraryMobileRemoteError(
                    message = state.remoteError.orEmpty(),
                    isLoading = state.isRemoteLoading,
                    onRetry = { onEvent(LibraryState.Event.RetrySelected) },
                )
            }
        }
    }
}

private fun Modifier.continueWatchingCardWidth(
    size: LibraryContinueWatchingCardSize,
): Modifier = when (size) {
    LibraryContinueWatchingCardSize.COMPACT -> widthIn(max = 280.dp).fillMaxWidth()
    LibraryContinueWatchingCardSize.STANDARD -> widthIn(max = 320.dp).fillMaxWidth()
    LibraryContinueWatchingCardSize.LARGE -> fillMaxWidth()
}

@Composable
private fun LibraryMobileEmptyList() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 72.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.library_mobile_empty_list),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
