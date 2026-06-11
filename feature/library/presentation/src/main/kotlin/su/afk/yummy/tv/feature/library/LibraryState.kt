package su.afk.yummy.tv.feature.library

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.core.storage.library.LibraryEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.anime.model.AnimePreview

enum class LibraryTab {
    CONTINUE_WATCHING,
    FAVORITES,
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
        val isRemoteLoading: Boolean = false,
        val remoteError: String? = null,
        val selectedTab: LibraryTab = LibraryTab.CONTINUE_WATCHING,
        val focusedItemId: Int? = null,
        val focusedPreview: AnimePreview? = null,
        val restoreFocusedItemOnEnter: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class AnimeSelected(val animeId: Int) : Event
        data class ContinueWatchingSelected(val entry: WatchProgressEntry) : Event
        data class RemoteAnimeSelected(val animeId: Int) : Event
        data class ItemFocused(val animeId: Int) : Event
        data class TabSelected(val tab: LibraryTab) : Event
        data object FocusedItemRestoreHandled : Event
        data object ScreenResumed : Event
        data object RetrySelected : Event
        data class RemoveLibraryEntry(val animeId: Int) : Event
        data class RemoveFavoriteEntry(val animeId: Int) : Event
        data class RemoveWatchProgress(val animeId: Int) : Event
        data class RemoveRemoteEntry(val animeId: Int, val list: UserAnimeList? = null, val favorite: Boolean = false) : Event
    }

    sealed interface Effect : UiEffect {
        data object ItemRemoved : Effect
    }
}
