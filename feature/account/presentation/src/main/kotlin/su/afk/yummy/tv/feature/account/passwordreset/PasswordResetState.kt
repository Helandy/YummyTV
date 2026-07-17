package su.afk.yummy.tv.feature.account.passwordreset

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.feature.account.account.YANI_HCAPTCHA_SITE_KEY

class PasswordResetState {
    data class State(
        val email: String = "",
        val isLoading: Boolean = false,
        val isCaptchaRequired: Boolean = false,
        val captchaSiteKey: String = YANI_HCAPTCHA_SITE_KEY,
        val captchaChallengeId: Int = 0,
        val isSuccess: Boolean = false,
        val validationError: Boolean = false,
        val requestError: Boolean = false,
        val captchaError: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data class EmailChanged(val value: String) : Event
        data object SubmitSelected : Event
        data class CaptchaSolved(val token: String) : Event
        data object CaptchaExpired : Event
        data object CaptchaFailed : Event
    }

    sealed interface Effect : UiEffect
}
