package su.afk.yummy.tv.feature.commonscreen.errorScreen

import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState

internal class ErrorScreenState {

    data class State(
        val error: ErrorItem? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data object Retry : Event
        data object Back : Event
    }

    sealed interface Effect : UiEffect
}
