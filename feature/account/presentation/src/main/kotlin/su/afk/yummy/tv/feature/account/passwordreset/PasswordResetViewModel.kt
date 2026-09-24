package su.afk.yummy.tv.feature.account.passwordreset

import android.util.Patterns
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.account.model.AccountCaptchaRequiredException
import su.afk.yummy.tv.feature.account.passwordreset.handler.PasswordResetHandler
import javax.inject.Inject

@HiltViewModel
class PasswordResetViewModel @Inject constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val handler: PasswordResetHandler,
) : BaseViewModel<PasswordResetState.State, PasswordResetState.Event, PasswordResetState.Effect>() {
    override fun createInitialState() = PasswordResetState.State()

    override fun onEvent(event: PasswordResetState.Event) {
        when (event) {
            PasswordResetState.Event.BackSelected -> nav.back()
            is PasswordResetState.Event.EmailChanged -> setState {
                copy(
                    email = event.value,
                    validationError = false,
                    requestError = false,
                    isCaptchaRequired = false,
                    captchaChallengeId = captchaChallengeId + 1,
                )
            }

            PasswordResetState.Event.SubmitSelected -> submit(null)
            is PasswordResetState.Event.CaptchaSolved -> {
                if (event.token.isBlank()) setState { copy(captchaError = true) }
                else submit(event.token)
            }

            PasswordResetState.Event.CaptchaExpired,
            PasswordResetState.Event.CaptchaFailed -> setState {
                copy(
                    isLoading = false,
                    captchaError = true,
                    captchaChallengeId = captchaChallengeId + 1,
                )
            }
        }
    }

    private fun submit(captchaResponse: String?) {
        val email = currentState.email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setState { copy(validationError = true) }
            return
        }
        if (currentState.isLoading) return
        viewModelScope.launch {
            setState {
                copy(
                    isLoading = true,
                    validationError = false,
                    requestError = false,
                    captchaError = false
                )
            }
            runSuspendCatching { handler.request(email, captchaResponse) }
                .onSuccess {
                    setState {
                        copy(isLoading = false, isCaptchaRequired = false, isSuccess = true)
                    }
                }
                .onFailure { error ->
                    if (error is AccountCaptchaRequiredException) {
                        setState {
                            copy(
                                isLoading = false,
                                isCaptchaRequired = true,
                                captchaChallengeId = captchaChallengeId + 1,
                                captchaError = captchaResponse != null,
                            )
                        }
                    } else {
                        setState { copy(isLoading = false, requestError = true) }
                    }
                }
        }
    }
}
