package su.afk.yummy.tv.feature.library.history

import androidx.compose.runtime.Immutable
import androidx.paging.PagingData
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.library.model.WatchHistoryEntry

class LibraryHistoryState {
    @Immutable
    data class State(
        val watchHistory: Flow<PagingData<WatchHistoryEntry>> = flowOf(PagingData.empty()),
        /** Локальный прогресс серий для полосок на карточках: ключ — historyProgressKey. */
        val localProgress: ImmutableMap<String, AnimeWatchProgress> = persistentMapOf(),
        val isSignedIn: Boolean = false,
    ) : UiState

    /** Пользовательские действия на экране истории просмотров. */
    sealed interface Event : UiEvent {
        /** Пользователь выбрал серию из истории — открываются серии тайтла с этой серией. */
        data class EntrySelected(val entry: WatchHistoryEntry) : Event

        /** Пользователь открыл страницу тайтла из записи истории. */
        data class DetailsSelected(val animeId: Int) : Event

        /** Экран снова стал активным: обновляем локальный прогресс серий. */
        data object ScreenResumed : Event
    }

    sealed interface Effect : UiEffect
}
