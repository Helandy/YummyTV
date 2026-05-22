package su.afk.yummy.tv.feature.details

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.anime.AnimePreview

class SimilarState {
    data class State(
        val similarState: SimilarUiState = SimilarUiState.Loading,
        val fromAi: Boolean = false,
        val focusedItemId: Int? = null,
        val focusedPreview: AnimePreview? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data class AnimeSelected(val animeId: Int) : Event
        data class ItemFocused(val animeId: Int) : Event
        data object SourceToggled : Event
    }

    sealed interface Effect : UiEffect
}
