package su.afk.yummy.tv.feature.details.viewingorder

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.anime.model.AnimeViewingOrderItem

class ViewingOrderState {
    data class State(
        val isLoading: Boolean = true,
        val currentAnimeId: Int = 0,
        val items: List<AnimeViewingOrderItem> = emptyList(),
        val error: String? = null,
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
