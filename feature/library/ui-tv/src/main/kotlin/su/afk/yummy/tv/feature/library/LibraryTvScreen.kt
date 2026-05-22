package su.afk.yummy.tv.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when (state.selectedTab) {
            LibraryTab.LIST -> LibraryGrid(
                items = state.items,
                focusedItemId = state.focusedItemId,
                focusedPreview = state.focusedPreview,
                gridFocusRequester = gridFocusRequester,
                onAnimeSelected = { onEvent(LibraryState.Event.AnimeSelected(it)) },
                onItemFocused = { onEvent(LibraryState.Event.ItemFocused(it)) },
                onRemoveLibraryEntry = { onEvent(LibraryState.Event.RemoveLibraryEntry(it)) },
            )
            LibraryTab.CONTINUE_WATCHING -> ContinueWatchingGrid(
                entries = state.continueWatching,
                focusedItemId = state.focusedItemId,
                focusedPreview = state.focusedPreview,
                gridFocusRequester = gridFocusRequester,
                onEntrySelected = { onEvent(LibraryState.Event.ContinueWatchingSelected(it)) },
                onItemFocused = { onEvent(LibraryState.Event.ItemFocused(it)) },
                onRemoveWatchProgress = { onEvent(LibraryState.Event.RemoveWatchProgress(it)) },
            )
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            LibrarySidePanel(
                selectedTab = state.selectedTab,
                onTabSelected = { onEvent(LibraryState.Event.TabSelected(it)) },
                contentFocusRequester = gridFocusRequester,
            )
        }
    }
}
