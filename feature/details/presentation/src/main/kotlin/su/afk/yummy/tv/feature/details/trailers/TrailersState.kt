package su.afk.yummy.tv.feature.details.trailers

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.core.model.anime.AnimeTrailer

class TrailersState {
    data class State(
        val isLoading: Boolean = true,
        val trailers: List<AnimeTrailer> = emptyList(),
        val error: String? = null,
    ) : UiState

    /** Пользовательские действия на экране трейлеров. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь запросил повторную загрузку трейлеров. */
        data object RetrySelected : Event
    }

    sealed interface Effect : UiEffect
}
