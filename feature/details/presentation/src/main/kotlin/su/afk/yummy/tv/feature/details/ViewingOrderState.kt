package su.afk.yummy.tv.feature.details

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

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data class AnimeSelected(val animeId: Int) : Event
    }

    sealed interface Effect : UiEffect
}
