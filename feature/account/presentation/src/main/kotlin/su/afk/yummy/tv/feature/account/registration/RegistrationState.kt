package su.afk.yummy.tv.feature.account.registration

import androidx.compose.runtime.Immutable
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.feature.account.account.model.AccountUiError

class RegistrationState {
    @Immutable
    data class State(
        val email: String = "",
        val username: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val isCaptchaRequired: Boolean = false,
        val captchaChallengeId: Int = 0,
        val error: AccountUiError? = null,
        val errorMessage: String? = null,
        val isSuccess: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data class EmailChanged(val email: String) : Event
        data class UsernameChanged(val username: String) : Event
        data class PasswordChanged(val password: String) : Event
        data object RegisterSelected : Event
        data class CaptchaSolved(val token: String) : Event
        data object CaptchaExpired : Event
        data class CaptchaFailed(val message: String? = null) : Event
    }

    sealed interface Effect : UiEffect {
        data object ShowCaptchaHint : Effect
        data object HideKeyboard : Effect

        /** Попытка не удалась — сбросить введённые данные, чтобы менеджер паролей не предложил их сохранить. */
        data object DiscardAutofill : Effect
    }
}
