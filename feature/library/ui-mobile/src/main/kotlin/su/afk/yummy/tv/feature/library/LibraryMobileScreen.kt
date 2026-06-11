package su.afk.yummy.tv.feature.library

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.feature.library.mobile.R
import su.afk.yummy.tv.feature.library.utils.mobileTitle
import su.afk.yummy.tv.feature.library.view.LibraryMobileDeleteButton
import su.afk.yummy.tv.feature.library.view.LibraryMobileLoadingIndicator
import su.afk.yummy.tv.feature.library.view.LibraryMobileRemoteError
import su.afk.yummy.tv.feature.library.view.LibraryMobileRemoveConfirmDialog
import su.afk.yummy.tv.feature.library.view.LibraryMobileTabs

private val libraryMobileTabs: List<LibraryTab>
    get() = LibraryTab.entries

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LibraryMobileScreen(

    state: LibraryState.State,
    effect: Flow<LibraryState.Effect>,
    onEvent: (LibraryState.Event) -> Unit,

) {
    val context = LocalContext.current
    val itemRemovedText = stringResource(R.string.library_mobile_remove_success)
    val pagerState = rememberPagerState(
        initialPage = state.selectedTab.toLibraryMobilePage(),
        pageCount = { libraryMobileTabs.size },
    )
    val coroutineScope = rememberCoroutineScope()
    var pendingRemoval by remember { mutableStateOf<PendingLibraryMobileRemoval?>(null) }

    LaunchedEffect(effect, context, itemRemovedText) {
        effect.collect { event ->
            when (event) {
                LibraryState.Effect.ItemRemoved -> {
                    Toast.makeText(context, itemRemovedText, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    pendingRemoval?.let { removal ->
        LibraryMobileRemoveConfirmDialog(
            title = removal.title,
            listTitle = removal.listTitle,
            onConfirm = {
                onEvent(removal.event())
                pendingRemoval = null
            },
            onDismiss = { pendingRemoval = null },
        )
    }

    LaunchedEffect(state.selectedTab) {
        val targetPage = state.selectedTab.toLibraryMobilePage()
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val selectedTab = pagerState.currentPage.toLibraryMobileTab()
        if (selectedTab != state.selectedTab) {
            onEvent(LibraryState.Event.TabSelected(selectedTab))
        }
    }

    BaseScreen(
        isScroll = false,
        topBar = { Text(stringResource(R.string.library_mobile_title)) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            LibraryMobileTabs(
                selectedTab = pagerState.currentPage.toLibraryMobileTab(),
                onSelected = { tab ->
                    val targetPage = tab.toLibraryMobilePage()
                    if (pagerState.currentPage != targetPage) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp),
            )

            HorizontalPager(
                state = pagerState,
                key = { page -> page.toLibraryMobileTab() },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            ) { page ->
                LibraryMobilePage(
                    tab = page.toLibraryMobileTab(),
                    state = state,
                    onEvent = onEvent,
                    onRemovalRequested = { pendingRemoval = it },
                )
            }
        }
    }
}

@Composable
private fun LibraryMobilePage(
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
                        posterUrl = item.posterMegaUrl ?: item.posterFullsizeUrl
                        ?: item.posterBigUrl ?: item.posterMediumUrl ?: item.posterSmallUrl,
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
                            posterUrl = item.poster?.mega ?: item.poster?.fullsize
                            ?: item.posterUrl,
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
                            posterUrl = item.posterMegaUrl ?: item.posterFullsizeUrl
                            ?: item.posterBigUrl ?: item.posterMediumUrl ?: item.posterSmallUrl,
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

private sealed interface PendingLibraryMobileRemoval {
    val title: String
    val listTitle: String

    fun event(): LibraryState.Event

    data class WatchProgress(
        val entry: WatchProgressEntry,
        override val listTitle: String,
    ) : PendingLibraryMobileRemoval {
        override val title = entry.animeTitle.ifBlank { entry.episode }
        override fun event(): LibraryState.Event =
            LibraryState.Event.RemoveWatchProgress(entry.animeId)
    }

    data class Favorite(
        val animeId: Int,
        override val title: String,
        override val listTitle: String,
        val isRemote: Boolean,
    ) : PendingLibraryMobileRemoval {
        override fun event(): LibraryState.Event =
            if (isRemote) {
                LibraryState.Event.RemoveRemoteEntry(animeId = animeId, favorite = true)
            } else {
                LibraryState.Event.RemoveFavoriteEntry(animeId)
            }
    }

    data class RemoteList(
        val item: UserAnimeListItem,
        override val listTitle: String,
    ) : PendingLibraryMobileRemoval {
        override val title = item.title
        override fun event(): LibraryState.Event =
            LibraryState.Event.RemoveRemoteEntry(item.animeId)
    }

    data class LocalList(
        val animeId: Int,
        override val title: String,
        override val listTitle: String,
    ) : PendingLibraryMobileRemoval {
        override fun event(): LibraryState.Event = LibraryState.Event.RemoveLibraryEntry(animeId)
    }
}

private fun LibraryTab.userAnimeList(): UserAnimeList? = when (this) {
    LibraryTab.WATCHING -> UserAnimeList.WATCHING
    LibraryTab.PLANNED -> UserAnimeList.PLANNED
    LibraryTab.COMPLETED -> UserAnimeList.COMPLETED
    LibraryTab.POSTPONED -> UserAnimeList.POSTPONED
    LibraryTab.DROPPED -> UserAnimeList.DROPPED
    LibraryTab.CONTINUE_WATCHING,
    LibraryTab.FAVORITES -> null
}

private fun LibraryTab.toLibraryMobilePage(): Int =
    libraryMobileTabs.indexOf(this).coerceAtLeast(0)

private fun Int.toLibraryMobileTab(): LibraryTab =
    libraryMobileTabs.getOrElse(this) { LibraryTab.CONTINUE_WATCHING }

private fun LibraryState.State.shouldShowRemoteLoader(tab: LibraryTab): Boolean {
    if (!isSignedIn || !isRemoteLoading || remoteError != null) return false
    return when (tab) {
        LibraryTab.CONTINUE_WATCHING -> false
        LibraryTab.FAVORITES -> items.none { it.isFavorite }
        LibraryTab.WATCHING,
        LibraryTab.PLANNED,
        LibraryTab.COMPLETED,
        LibraryTab.POSTPONED,
        LibraryTab.DROPPED -> remoteItems[tab].orEmpty().isEmpty()
    }
}
