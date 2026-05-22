package su.afk.yummy.tv.feature.library

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.core.storage.library.LibraryEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.anime.AnimePreview

enum class LibraryTab {
    LIST,
    CONTINUE_WATCHING,
}

class LibraryState {
    data class State(
        val items: List<LibraryEntry> = emptyList(),
        val continueWatching: List<WatchProgressEntry> = emptyList(),
        val selectedTab: LibraryTab = LibraryTab.LIST,
        val focusedItemId: Int? = null,
        val focusedPreview: AnimePreview? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data class AnimeSelected(val animeId: Int) : Event
        data class ContinueWatchingSelected(val entry: WatchProgressEntry) : Event
        data class ItemFocused(val animeId: Int) : Event
        data class TabSelected(val tab: LibraryTab) : Event
        data class RemoveLibraryEntry(val animeId: Int) : Event
        data class RemoveWatchProgress(val animeId: Int) : Event
    }

    sealed interface Effect : UiEffect
}
