package su.afk.yummy.tv.feature.details

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.anime.AnimeDetails

class FullDetailsState {
    data class State(
        val isLoading: Boolean = true,
        val details: AnimeDetails? = null,
        val error: String? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetrySelected : Event
    }

    sealed interface Effect : UiEffect
}
