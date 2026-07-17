package su.afk.yummy.tv.feature.messages.dialogs

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.messages.model.DialogSummary

object DialogsState {
    data class State(
        val isAuthResolved: Boolean = false,
        val isAuthorized: Boolean = false,
        val dialogs: Flow<PagingData<DialogSummary>> = flowOf(PagingData.empty()),
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object LoginSelected : Event
        data class DialogSelected(val userId: Int) : Event
    }

    sealed interface Effect : UiEffect
}
