package su.afk.yummy.tv.feature.details.viewingorder

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.core.model.anime.AnimeViewingOrderItem

class ViewingOrderState {
    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val currentAnimeId: Int = 0,
        val items: ImmutableList<AnimeViewingOrderItem> = persistentListOf(),
        val error: UiText? = null,
    ) : UiState

    /** Пользовательские действия на экране порядка просмотра. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь выбрал аниме с указанным идентификатором. */
        data class AnimeSelected(val animeId: Int) : Event

        /** Пользователь запросил повторную загрузку порядка просмотра. */
        data object RetrySelected : Event
    }

    sealed interface Effect : UiEffect
}
