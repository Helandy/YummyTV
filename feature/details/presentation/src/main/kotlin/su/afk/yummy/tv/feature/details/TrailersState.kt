package su.afk.yummy.tv.feature.details

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.anime.model.AnimeTrailer

class TrailersState {
    data class State(
        val isLoading: Boolean = true,
        val trailers: List<AnimeTrailer> = emptyList(),
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
    }

    sealed interface Effect : UiEffect
}
