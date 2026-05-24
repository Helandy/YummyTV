package su.afk.yummy.tv.feature.details

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.account.AnimeListStats
import su.afk.yummy.tv.domain.account.AnimeRatingSummary

class RatingState {
    data class State(
        val isLoading: Boolean = true,
        val error: String? = null,
        val ratingSummary: AnimeRatingSummary = AnimeRatingSummary(),
        val listStats: AnimeListStats = AnimeListStats(),
        val selectedUserRating: Int? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetrySelected : Event
        data class RatingSelected(val rating: Int) : Event
        data object RatingDeleted : Event
    }

    sealed interface Effect : UiEffect
}
