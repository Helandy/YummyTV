package su.afk.yummy.tv.feature.library

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.core.storage.library.LibraryEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry

enum class LibraryTab {
    CONTINUE_WATCHING,
    FAVORITES,
    WATCHING,
    PLANNED,
    COMPLETED,
    POSTPONED,
    DROPPED,
}

enum class LibraryRemoveTarget {
    LIST,
    FAVORITE,
}

class LibraryState {
    data class State(
        val items: List<LibraryEntry> = emptyList(),
        val continueWatching: List<WatchProgressEntry> = emptyList(),
        val isSignedIn: Boolean = false,
        val isRemoteLoading: Boolean = false,
        val remoteError: String? = null,
        val selectedTab: LibraryTab = LibraryTab.CONTINUE_WATCHING,
        val focusedItemId: Int? = null,
        val restoreFocusedItemOnEnter: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class AnimeSelected(val animeId: Int) : Event
        data class ContinueWatchingSelected(val entry: WatchProgressEntry) : Event
        data class ItemFocused(val animeId: Int) : Event
        data class TabSelected(val tab: LibraryTab) : Event
        data object FocusedItemRestoreHandled : Event
        data object ScreenResumed : Event
        data object RetrySelected : Event
        data class RemoveEntry(val animeId: Int, val target: LibraryRemoveTarget) : Event
        data class RemoveWatchProgress(val animeId: Int) : Event
    }

    sealed interface Effect : UiEffect {
        data object ItemRemoved : Effect
    }
}
