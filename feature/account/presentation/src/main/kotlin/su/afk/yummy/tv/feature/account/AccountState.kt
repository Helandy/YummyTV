package su.afk.yummy.tv.feature.account

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState

class AccountState {
    data class State(
        val accessToken: String = "",
        val userId: Int = 0,
        val nickname: String = "",
        val login: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data class LoginChanged(val login: String) : Event
        data class PasswordChanged(val password: String) : Event
        data object LoginSelected : Event
        data object LogoutSelected : Event
        data object RefreshProfileSelected : Event
    }

    sealed interface Effect : UiEffect
}
