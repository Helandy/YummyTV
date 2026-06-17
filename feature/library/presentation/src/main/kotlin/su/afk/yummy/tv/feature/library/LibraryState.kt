package su.afk.yummy.tv.feature.library

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.core.preferences.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.library.model.LibraryItem

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
        val items: List<LibraryItem> = emptyList(),
        val continueWatching: List<HomeContinueWatchingItem> = emptyList(),
        val isSignedIn: Boolean = false,
        val isRemoteLoading: Boolean = false,
        val remoteError: String? = null,
        val selectedTab: LibraryTab = LibraryTab.CONTINUE_WATCHING,
        val focusedItemId: Int? = null,
        val restoreFocusedItemOnEnter: Boolean = false,
        val continueWatchingCardSize: LibraryContinueWatchingCardSize =
            LibraryContinueWatchingCardSize.LARGE,
    ) : UiState

    /** Пользовательские действия на экране библиотеки. */
    sealed interface Event : UiEvent {
        /** Пользователь выбрал аниме с указанным идентификатором. */
        data class AnimeSelected(val animeId: Int) : Event

        /** Пользователь выбрал элемент продолжения просмотра. */
        data class ContinueWatchingSelected(val entry: HomeContinueWatchingItem) : Event

        /** Пользователь открыл детали элемента продолжения просмотра. */
        data class ContinueWatchingDetailsSelected(val entry: HomeContinueWatchingItem) : Event

        /** Фокус переместился на аниме с указанным идентификатором. */
        data class ItemFocused(val animeId: Int) : Event

        /** Пользователь выбрал вкладку библиотеки. */
        data class TabSelected(val tab: LibraryTab) : Event

        /** UI завершил восстановление фокуса на элементе. */
        data object FocusedItemRestoreHandled : Event

        /** Экран снова стал активным для пользователя. */
        data object ScreenResumed : Event

        /** Пользователь запросил повторную загрузку библиотеки. */
        data object RetrySelected : Event

        /** Пользователь удалил тайтл из указанной части библиотеки. */
        data class RemoveEntry(val animeId: Int, val target: LibraryRemoveTarget) : Event

        /** Пользователь удалил локальный прогресс просмотра тайтла. */
        data class RemoveWatchProgress(val animeId: Int) : Event
    }

    sealed interface Effect : UiEffect {
        data object ItemRemoved : Effect
    }
}
