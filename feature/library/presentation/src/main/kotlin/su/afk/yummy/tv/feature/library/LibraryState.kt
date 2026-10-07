package su.afk.yummy.tv.feature.library

import androidx.compose.runtime.Immutable
import androidx.paging.PagingData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.LibrarySort
import su.afk.yummy.tv.core.model.settings.LibrarySortDirection
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.WatchHistoryEntry
import su.afk.yummy.tv.feature.library.model.LibraryRemoveTarget
import su.afk.yummy.tv.feature.library.model.LibraryTab

class LibraryState {
    @Immutable
    data class State(
        val items: ImmutableList<LibraryItem> = persistentListOf(),
        val tabItems: ImmutableMap<LibraryTab, ImmutableList<LibraryItem>> = persistentMapOf(),
        val continueWatching: ImmutableList<HomeContinueWatchingItem> = persistentListOf(),
        /** Тайтл, для которого сейчас резолвится запуск плеера из «Продолжить просмотр». */
        val launchingContinueWatchingAnimeId: Int? = null,
        val watchHistory: Flow<PagingData<WatchHistoryEntry>> = flowOf(PagingData.empty()),
        val historyLocalProgress: ImmutableMap<String, AnimeWatchProgress> = persistentMapOf(),
        val isSignedIn: Boolean = false,
        val isRemoteLoading: Boolean = false,
        val remoteError: String? = null,
        /** Вкладки в порядке, выбранном пользователем в настройках. */
        val tabs: ImmutableList<LibraryTab> = LibraryTab.entries.toImmutableList(),
        val selectedTab: LibraryTab = LibraryTab.CONTINUE_WATCHING,
        val continueWatchingCardSize: LibraryContinueWatchingCardSize =
            LibraryContinueWatchingCardSize.LARGE,
        val showTitleYear: Boolean = false,
        val sort: LibrarySort = LibrarySort.ADDED_DATE,
        val sortDirection: LibrarySortDirection = LibrarySortDirection.DESC,
    ) : UiState

    /** Пользовательские действия на экране библиотеки. */
    sealed interface Event : UiEvent {
        /** Пользователь выбрал аниме с указанным идентификатором. */
        data class AnimeSelected(val animeId: Int) : Event

        /** Пользователь выбрал элемент продолжения просмотра. */
        data class ContinueWatchingSelected(val entry: HomeContinueWatchingItem) : Event

        /** Пользователь открыл детали элемента продолжения просмотра. */
        data class ContinueWatchingDetailsSelected(val entry: HomeContinueWatchingItem) : Event

        data class HistorySelected(val entry: WatchHistoryEntry) : Event

        data class HistoryDetailsSelected(val animeId: Int) : Event

        /** Пользователь выбрал вкладку библиотеки. */
        data class TabSelected(val tab: LibraryTab) : Event

        /** Пользователь выбрал поле сортировки списков библиотеки. */
        data class SortSelected(val sort: LibrarySort) : Event

        /** Пользователь развернул порядок сортировки. */
        data object SortDirectionToggled : Event

        /** Экран снова стал активным для пользователя. */
        data object ScreenResumed : Event

        /** Пользователь запросил повторную загрузку библиотеки. */
        data object RetrySelected : Event

        /** Пользователь удалил тайтл из указанной части библиотеки. */
        data class RemoveEntry(val animeId: Int, val target: LibraryRemoveTarget) : Event

        /** Пользователь скрыл запись продолжения просмотра. */
        data class RemoveWatchProgress(val entry: HomeContinueWatchingItem) : Event
    }

    sealed interface Effect : UiEffect {
        data object ItemRemoved : Effect
        data class ShowToast(val message: String) : Effect
    }
}
