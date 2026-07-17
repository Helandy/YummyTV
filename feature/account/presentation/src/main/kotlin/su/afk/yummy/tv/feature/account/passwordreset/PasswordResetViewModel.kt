package su.afk.yummy.tv.feature.account.passwordreset

import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.account.model.AccountCaptchaRequiredException
import su.afk.yummy.tv.feature.account.passwordreset.handler.PasswordResetHandler
import javax.inject.Inject

@HiltViewModel
class PasswordResetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val handler: PasswordResetHandler,
) : BaseViewModelNew<PasswordResetState.State, PasswordResetState.Event, PasswordResetState.Effect>(
    savedStateHandle
) {
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
            runCatching { handler.request(email, captchaResponse) }
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
