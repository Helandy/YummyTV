package su.afk.yummy.tv.feature.library

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.core.storage.library.LibraryEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.account.UserAnimeList
import su.afk.yummy.tv.domain.account.UserAnimeListItem
import su.afk.yummy.tv.domain.anime.AnimePreview

enum class LibraryTab {
    CONTINUE_WATCHING,
    WATCHING,
    PLANNED,
    COMPLETED,
    POSTPONED,
    DROPPED,
}

class LibraryState {
    data class State(
        val items: List<LibraryEntry> = emptyList(),
        val continueWatching: List<WatchProgressEntry> = emptyList(),
        val remoteItems: Map<LibraryTab, List<UserAnimeListItem>> = emptyMap(),
        val isSignedIn: Boolean = false,
        val remoteError: String? = null,
        val selectedTab: LibraryTab = LibraryTab.CONTINUE_WATCHING,
        val focusedItemId: Int? = null,
        val focusedPreview: AnimePreview? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data class AnimeSelected(val animeId: Int) : Event
        data class ContinueWatchingSelected(val entry: WatchProgressEntry) : Event
        data class RemoteAnimeSelected(val animeId: Int) : Event
        data class ItemFocused(val animeId: Int) : Event
        data class TabSelected(val tab: LibraryTab) : Event
        data class RemoveLibraryEntry(val animeId: Int) : Event
        data class RemoveWatchProgress(val animeId: Int) : Event
        data class RemoveRemoteEntry(val animeId: Int, val list: UserAnimeList? = null, val favorite: Boolean = false) : Event
    }

    sealed interface Effect : UiEffect
}
