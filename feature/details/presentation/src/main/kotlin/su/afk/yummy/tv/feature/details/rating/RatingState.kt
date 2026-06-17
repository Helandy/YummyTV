package su.afk.yummy.tv.feature.details.rating

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary

class RatingState {
    data class State(
        val isLoading: Boolean = true,
        val error: String? = null,
        val ratingSummary: AnimeRatingSummary = AnimeRatingSummary(),
        val listStats: AnimeListStats = AnimeListStats(),
        val selectedUserRating: Int? = null,
    ) : UiState

    /** Пользовательские действия на экране рейтинга тайтла. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь запросил повторную загрузку рейтинга. */
        data object RetrySelected : Event

        /** Пользователь выбрал оценку для тайтла. */
        data class RatingSelected(val rating: Int) : Event

        /** Пользователь удалил свою оценку тайтла. */
        data object RatingDeleted : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
    }
}
