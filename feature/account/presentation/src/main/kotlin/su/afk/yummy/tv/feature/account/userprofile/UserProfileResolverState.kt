package su.afk.yummy.tv.feature.account.userprofile

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState

class UserProfileResolverState {
    data class State(val isLoading: Boolean = true, val hasError: Boolean = false) : UiState
    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetrySelected : Event
    }

    sealed interface Effect : UiEffect
}
