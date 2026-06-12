package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.feature.library.LibraryState
import su.afk.yummy.tv.feature.library.LibraryTab
import su.afk.yummy.tv.feature.library.mobile.R
import su.afk.yummy.tv.feature.library.model.PendingLibraryMobileRemoval
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

    MobilePosterGrid(contentPadding = PaddingValues()) {
        if (showRemoteLoader) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LibraryMobileLoadingIndicator()
            }
        }

        when (tab) {
            LibraryTab.CONTINUE_WATCHING -> {
                items(state.continueWatching, key = { "${it.animeId}-${it.episode}" }) { entry ->
                    MobilePosterCard(
                        title = entry.animeTitle.ifBlank {
                            stringResource(R.string.library_mobile_episode, entry.episode)
                        },
                        posterUrl = entry.posterUrl.ifBlank { null },
                        subtitle = stringResource(R.string.library_mobile_episode, entry.episode),
                        posterOverlay = {
                            LibraryMobileDeleteButton(
                                onClick = {
                                    onRemovalRequested(
                                        PendingLibraryMobileRemoval.WatchProgress(
                                            entry = entry,
                                            listTitle = continueWatchingTitle,
                                        ),
                                    )
                                },
                            )
                        },
                        onClick = { onEvent(LibraryState.Event.ContinueWatchingSelected(entry)) },
                    )
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
                                            isRemote = state.isSignedIn,
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
                if (state.isSignedIn) {
                    val remote = state.remoteItems[tab].orEmpty()
                    items(remote, key = { it.animeId }) { item ->
                        MobilePosterCard(
                            title = item.title,
                            posterUrl = item.posterUrl(),
                            rating = item.rating,
                            posterOverlay = {
                                LibraryMobileDeleteButton(
                                    onClick = {
                                        onRemovalRequested(
                                            PendingLibraryMobileRemoval.RemoteList(
                                                item = item,
                                                listTitle = selectedTabTitle,
                                            ),
                                        )
                                    },
                                )
                            },
                            onClick = { onEvent(LibraryState.Event.RemoteAnimeSelected(item.animeId)) },
                        )
                    }
                } else {
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
                                            PendingLibraryMobileRemoval.LocalList(
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
