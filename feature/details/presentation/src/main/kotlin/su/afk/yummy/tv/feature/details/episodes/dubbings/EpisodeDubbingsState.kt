package su.afk.yummy.tv.feature.details.episodes.dubbings

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState

class EpisodeDubbingsState {
    data class State(
        val episode: String = "",
        val isLoading: Boolean = true,
        val error: String? = null,
        val dubbings: List<String> = emptyList(),
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
    }

    sealed interface Effect : UiEffect
}
