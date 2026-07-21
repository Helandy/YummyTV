package su.afk.yummy.tv.feature.library.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTitleListCard
import su.afk.yummy.tv.feature.library.LibraryState
import su.afk.yummy.tv.feature.library.LibraryTab
import su.afk.yummy.tv.feature.library.mobile.R
import su.afk.yummy.tv.feature.library.mobile.model.PendingLibraryMobileRemoval
import su.afk.yummy.tv.feature.library.mobile.utils.mobileDateText
import su.afk.yummy.tv.feature.library.mobile.utils.mobileTabItemCount
import su.afk.yummy.tv.feature.library.mobile.utils.mobileTitle
import su.afk.yummy.tv.feature.library.mobile.utils.mobileUserRating
import su.afk.yummy.tv.feature.library.mobile.utils.posterUrl
import su.afk.yummy.tv.feature.library.mobile.utils.shouldShowRemoteLoader
import su.afk.yummy.tv.feature.library.mobile.utils.userAnimeList

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

    if (tab == LibraryTab.CONTINUE_WATCHING) {
        LibraryMobileContinueWatchingGrid(
            entries = state.continueWatching,
            cardSize = state.continueWatchingCardSize,
            showRemoteLoader = showRemoteLoader,
            isEmpty = isEmpty,
            remoteError = state.remoteError.takeIf { tab == state.selectedTab },
            isRemoteLoading = state.isRemoteLoading,
            onRetry = { onEvent(LibraryState.Event.RetrySelected) },
            onEntrySelected = { onEvent(LibraryState.Event.ContinueWatchingSelected(it)) },
            onDetailsSelected = {
                onEvent(LibraryState.Event.ContinueWatchingDetailsSelected(it))
            },
            onDeleteSelected = { entry ->
                onRemovalRequested(
                    PendingLibraryMobileRemoval.WatchProgress(
                        entry = entry,
                        listTitle = continueWatchingTitle,
                    ),
                )
            },
        )
        return
    }

    if (tab == LibraryTab.HISTORY) {
        LibraryMobileHistoryPage(
            history = state.watchHistory,
            isSignedIn = state.isSignedIn,
            onEntrySelected = { onEvent(LibraryState.Event.HistorySelected(it)) },
            onDetailsSelected = { onEvent(LibraryState.Event.HistoryDetailsSelected(it.animeId)) },
        )
        return
    }

    val libraryItems = when (tab) {
        LibraryTab.FAVORITES -> state.items.filter { it.isFavorite }
        else -> {
            val localList = tab.userAnimeList()
            state.items.filter { it.listId == localList?.id }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = MobileBottomBarDefaults.PosterGridContentBottomPadding +
                    MobileBottomBarDefaults.ExtraContentBottomPadding + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showRemoteLoader) {
            item(key = "remote_loader") {
                LibraryMobileLoadingIndicator()
            }
        }

        if (isEmpty && !showRemoteLoader) {
            item(key = "empty") {
                LibraryMobileEmptyList()
            }
        }

        items(libraryItems, key = { it.animeId }) { item ->
            MobileTitleListCard(
                title = item.title,
                posterUrl = item.posterUrl(),
                dateText = item.mobileDateText(tab),
                rating = item.mobileUserRating(),
                contentOverlay = {
                    LibraryMobileDeleteButton(
                        onClick = {
                            val removal = if (tab == LibraryTab.FAVORITES) {
                                PendingLibraryMobileRemoval.Favorite(
                                    animeId = item.animeId,
                                    title = item.title,
                                    listTitle = favoritesTitle,
                                )
                            } else {
                                PendingLibraryMobileRemoval.ListEntry(
                                    animeId = item.animeId,
                                    title = item.title,
                                    listTitle = selectedTabTitle,
                                )
                            }
                            onRemovalRequested(removal)
                        },
                    )
                },
                onClick = { onEvent(LibraryState.Event.AnimeSelected(item.animeId)) },
            )
        }

        if (tab == state.selectedTab && state.remoteError != null) {
            item(key = "remote_error") {
                LibraryMobileRemoteError(
                    message = state.remoteError.orEmpty(),
                    isLoading = state.isRemoteLoading,
                    onRetry = { onEvent(LibraryState.Event.RetrySelected) },
                )
            }
        }
    }
}

@Composable
internal fun LibraryMobileEmptyList() {
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
