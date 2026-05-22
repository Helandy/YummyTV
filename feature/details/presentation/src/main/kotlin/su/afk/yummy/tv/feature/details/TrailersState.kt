package su.afk.yummy.tv.feature.details

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.anime.AnimeTrailer

class TrailersState {
    data class State(
        val isLoading: Boolean = true,
        val animeTitle: String = "",
        val trailers: List<AnimeTrailer> = emptyList(),
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data class TrailerSelected(val iframeUrl: String) : Event
    }

    sealed interface Effect : UiEffect
}
