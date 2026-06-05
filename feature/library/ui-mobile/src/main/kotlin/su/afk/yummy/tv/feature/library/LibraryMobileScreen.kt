package su.afk.yummy.tv.feature.library

import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.feature.library.mobile.R
import su.afk.yummy.tv.feature.library.utils.mobileTitle
import su.afk.yummy.tv.feature.library.view.LibraryMobileDeleteButton
import su.afk.yummy.tv.feature.library.view.LibraryMobileRemoveConfirmDialog
import su.afk.yummy.tv.feature.library.view.LibraryMobileTabs

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LibraryMobileScreen(

    state: LibraryState.State,
    effect: Flow<LibraryState.Effect>,
    onEvent: (LibraryState.Event) -> Unit,

) {
    val context = LocalContext.current
    val itemRemovedText = stringResource(R.string.library_mobile_remove_success)
    val continueWatchingTitle = LibraryTab.CONTINUE_WATCHING.mobileTitle()
    val favoritesTitle = LibraryTab.FAVORITES.mobileTitle()
    val selectedTabTitle = state.selectedTab.mobileTitle()
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

    BaseScreen(
        isScroll = false,
        topBar = { Text(stringResource(R.string.library_mobile_title)) },
    ) {
        MobilePosterGrid(contentPadding = PaddingValues()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            LibraryMobileTabs(
                selectedTab = state.selectedTab,
                onSelected = { onEvent(LibraryState.Event.TabSelected(it)) },
                modifier = Modifier
                    .fillMaxWidth(),
            )
        }
        when (state.selectedTab) {
            LibraryTab.CONTINUE_WATCHING -> {
                items(state.continueWatching, key = { "${it.animeId}-${it.episode}" }) { entry ->
                    MobilePosterCard(
                        title = entry.animeTitle.ifBlank { stringResource(R.string.library_mobile_episode, entry.episode) },
                        posterUrl = entry.posterUrl.ifBlank { null },
                        subtitle = stringResource(R.string.library_mobile_episode, entry.episode),
                        posterOverlay = {
                            LibraryMobileDeleteButton(
                                onClick = {
                                    pendingRemoval = PendingLibraryMobileRemoval.WatchProgress(
                                        entry = entry,
                                        listTitle = continueWatchingTitle,
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
                        posterUrl = item.posterMegaUrl ?: item.posterFullsizeUrl ?: item.posterBigUrl ?: item.posterMediumUrl ?: item.posterSmallUrl,
                        posterOverlay = {
                            LibraryMobileDeleteButton(
                                onClick = {
                                    pendingRemoval = PendingLibraryMobileRemoval.Favorite(
                                        animeId = item.animeId,
                                        title = item.title,
                                        listTitle = favoritesTitle,
                                        isRemote = state.isSignedIn,
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
                    val remote = state.remoteItems[state.selectedTab].orEmpty()
                    items(remote, key = { it.animeId }) { item ->
                        MobilePosterCard(
                            title = item.title,
                            posterUrl = item.poster?.mega ?: item.poster?.fullsize
                            ?: item.posterUrl,
                            rating = item.rating,
                            posterOverlay = {
                                LibraryMobileDeleteButton(
                                    onClick = {
                                        pendingRemoval = PendingLibraryMobileRemoval.RemoteList(
                                            item = item,
                                            listTitle = selectedTabTitle,
                                        )
                                    },
                                )
                            },
                            onClick = { onEvent(LibraryState.Event.RemoteAnimeSelected(item.animeId)) },
                        )
                    }
                } else {
                    val localList = state.selectedTab.userAnimeList()
                    val localItems = state.items.filter { it.listId == localList?.id }
                    items(localItems, key = { it.animeId }) { item ->
                        MobilePosterCard(
                            title = item.title,
                            posterUrl = item.posterMegaUrl ?: item.posterFullsizeUrl
                            ?: item.posterBigUrl ?: item.posterMediumUrl ?: item.posterSmallUrl,
                            posterOverlay = {
                                LibraryMobileDeleteButton(
                                    onClick = {
                                        pendingRemoval = PendingLibraryMobileRemoval.LocalList(
                                            animeId = item.animeId,
                                            title = item.title,
                                            listTitle = selectedTabTitle,
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
        if (state.remoteError != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Button(onClick = { onEvent(LibraryState.Event.ScreenResumed) }) {
                    Text(stringResource(R.string.library_mobile_retry_error, state.remoteError.orEmpty()))
                }
            }
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
