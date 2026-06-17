package su.afk.yummy.tv.feature.details.trailers

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.anime.model.AnimeTrailer

class TrailersState {
    data class State(
        val isLoading: Boolean = true,
        val trailers: List<AnimeTrailer> = emptyList(),
    ) : UiState

    /** Пользовательские действия на экране трейлеров. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event
    }

    sealed interface Effect : UiEffect
}
