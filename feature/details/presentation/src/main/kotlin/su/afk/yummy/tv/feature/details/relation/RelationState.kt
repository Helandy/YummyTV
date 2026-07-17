package su.afk.yummy.tv.feature.details.relation

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.anime.model.AnimeRelation

class RelationState {
    data class State(
        val isLoading: Boolean = true,
        val relation: AnimeRelation? = null,
        val error: String? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetrySelected : Event
        data class AnimeSelected(val animeId: Int) : Event
        data class SubGenreSelected(val id: Int) : Event
    }

    sealed interface Effect : UiEffect
}
