package su.afk.yummy.tv.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.core.storage.library.LibraryEntry
import su.afk.yummy.tv.domain.account.UserAnimeList
import su.afk.yummy.tv.feature.library.view.ContinueWatchingGrid
import su.afk.yummy.tv.feature.library.view.LibraryGrid
import su.afk.yummy.tv.feature.library.view.LibrarySidePanel

internal val CollapsedPanelWidth = 52.dp
internal val InProgressColor = Color(0xFF4CAF50)

@Composable
fun LibraryTvScreen(
    state: LibraryState.State,
    effect: Flow<LibraryState.Effect>,
    onEvent: (LibraryState.Event) -> Unit,
) {
    val gridFocusRequester = remember { FocusRequester() }
    val selectedTabFocusRequester = remember { FocusRequester() }
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var restoreGridFocusOnResume by rememberSaveable { mutableStateOf(false) }
    var restoreContinueWatchingFirstOnResume by rememberSaveable { mutableStateOf(false) }
    var continueWatchingRestoreFirstToken by rememberSaveable { mutableIntStateOf(0) }
    val hasFocusableGridContent = when (state.selectedTab) {
        LibraryTab.CONTINUE_WATCHING -> state.continueWatching.isNotEmpty()
        LibraryTab.PLANNED,
        LibraryTab.COMPLETED,
        LibraryTab.DROPPED,
        LibraryTab.POSTPONED,
        LibraryTab.WATCHING -> if (state.isSignedIn) {
            state.remoteItems[state.selectedTab].orEmpty().isNotEmpty()
        } else {
            val localListId = state.selectedTab.userAnimeListId()
            localListId != null && state.items.any { it.listId == localListId }
        }
    }

    DisposableEffect(gridFocusRequester, hasFocusableGridContent, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(if (hasFocusableGridContent) gridFocusRequester else null)
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    DisposableEffect(lifecycleOwner, hasFocusableGridContent) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && restoreGridFocusOnResume && hasFocusableGridContent) {
                if (restoreContinueWatchingFirstOnResume && state.selectedTab == LibraryTab.CONTINUE_WATCHING) {
                    restoreContinueWatchingFirstOnResume = false
                    continueWatchingRestoreFirstToken += 1
                }
                restoreGridFocusOnResume = false
                scope.launch {
                    delay(60)
                    runCatching { gridFocusRequester.requestFocus() }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when (state.selectedTab) {
            LibraryTab.CONTINUE_WATCHING -> ContinueWatchingGrid(
                entries = state.continueWatching,
                focusedItemId = state.focusedItemId,
                focusedPreview = state.focusedPreview,
                gridFocusRequester = gridFocusRequester,
                sidePanelFocusRequester = selectedTabFocusRequester,
                restoreFirstItemToken = continueWatchingRestoreFirstToken,
                onEntrySelected = {
                    restoreGridFocusOnResume = true
                    restoreContinueWatchingFirstOnResume = true
                    onEvent(LibraryState.Event.ContinueWatchingSelected(it))
                },
                onItemFocused = { onEvent(LibraryState.Event.ItemFocused(it)) },
                onRemoveWatchProgress = { onEvent(LibraryState.Event.RemoveWatchProgress(it)) },
            )
            LibraryTab.WATCHING,
            LibraryTab.PLANNED,
            LibraryTab.COMPLETED,
            LibraryTab.POSTPONED,
            LibraryTab.DROPPED -> {
                if (!state.isSignedIn) {
                    val localListId = state.selectedTab.userAnimeListId()
                    LibraryGrid(
                        items = state.items.filter { it.listId == localListId },
                        focusedItemId = state.focusedItemId,
                        focusedPreview = state.focusedPreview,
                        gridFocusRequester = gridFocusRequester,
                        sidePanelFocusRequester = selectedTabFocusRequester,
                        onAnimeSelected = {
                            restoreGridFocusOnResume = true
                            onEvent(LibraryState.Event.AnimeSelected(it))
                        },
                        onItemFocused = { onEvent(LibraryState.Event.ItemFocused(it)) },
                        onRemoveLibraryEntry = { onEvent(LibraryState.Event.RemoveLibraryEntry(it)) },
                    )
                } else {
                    val remoteItems = state.remoteItems[state.selectedTab].orEmpty()
                    LibraryGrid(
                        items = remoteItems.map { LibraryEntry(it.animeId, it.title, it.posterUrl) },
                        focusedItemId = state.focusedItemId,
                        focusedPreview = state.focusedPreview,
                        gridFocusRequester = gridFocusRequester,
                        sidePanelFocusRequester = selectedTabFocusRequester,
                        onAnimeSelected = {
                            restoreGridFocusOnResume = true
                            onEvent(LibraryState.Event.RemoteAnimeSelected(it))
                        },
                        onItemFocused = { onEvent(LibraryState.Event.ItemFocused(it)) },
                        onRemoveLibraryEntry = {
                            onEvent(
                                LibraryState.Event.RemoveRemoteEntry(
                                    animeId = it,
                                )
                            )
                        },
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            LibrarySidePanel(
                selectedTab = state.selectedTab,
                onTabSelected = { onEvent(LibraryState.Event.TabSelected(it)) },
                contentFocusRequester = gridFocusRequester,
                selectedTabFocusRequester = selectedTabFocusRequester,
            )
        }
    }
}

private fun LibraryTab.userAnimeListId(): Int? = when (this) {
    LibraryTab.CONTINUE_WATCHING -> null
    LibraryTab.WATCHING -> UserAnimeList.WATCHING.id
    LibraryTab.PLANNED -> UserAnimeList.PLANNED.id
    LibraryTab.COMPLETED -> UserAnimeList.COMPLETED.id
    LibraryTab.POSTPONED -> UserAnimeList.POSTPONED.id
    LibraryTab.DROPPED -> UserAnimeList.DROPPED.id
}
