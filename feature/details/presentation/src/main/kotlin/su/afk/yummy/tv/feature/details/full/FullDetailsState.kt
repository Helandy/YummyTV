package su.afk.yummy.tv.feature.details.full

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.anime.model.AnimeDetails

class FullDetailsState {
    data class State(
        val isLoading: Boolean = true,
        val details: AnimeDetails? = null,
        val error: String? = null,
    ) : UiState

    /** Пользовательские действия на экране полного описания. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь запросил повторную загрузку описания. */
        data object RetrySelected : Event
    }

    sealed interface Effect : UiEffect
}
