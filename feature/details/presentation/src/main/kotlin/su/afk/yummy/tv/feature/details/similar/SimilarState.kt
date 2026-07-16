package su.afk.yummy.tv.feature.details.similar

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.feature.details.details.SimilarUiState

class SimilarState {
    data class State(
        val similarState: SimilarUiState = SimilarUiState.Loading,
        val fromAi: Boolean = false,
    ) : UiState

    /** Пользовательские действия на экране похожих аниме. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь выбрал аниме с указанным идентификатором. */
        data class AnimeSelected(val animeId: Int) : Event

        /** Пользователь выбрал источник рекомендаций. */
        data class SourceSelected(val fromAi: Boolean) : Event

        /** Пользователь переключил источник рекомендаций. */
        data object SourceToggled : Event

        /** Пользователь запросил повторную загрузку рекомендаций. */
        data object RetrySelected : Event
    }

    sealed interface Effect : UiEffect
}
