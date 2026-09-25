package su.afk.yummy.tv.feature.account.registration

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.account.model.AccountCaptchaRequiredException
import su.afk.yummy.tv.domain.account.model.RegistrationException
import su.afk.yummy.tv.domain.account.model.UserRegistration
import su.afk.yummy.tv.domain.account.usecase.RegisterUserUseCase
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.utils.validateRegistrationForm
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val registerUserUseCase: RegisterUserUseCase,
) : BaseViewModel<RegistrationState.State, RegistrationState.Event, RegistrationState.Effect>() {

    override fun createInitialState() = RegistrationState.State()

    override fun onEvent(event: RegistrationState.Event) {
        when (event) {
            RegistrationState.Event.BackSelected -> nav.back()
            is RegistrationState.Event.EmailChanged -> setState { copy(email = event.email, error = null, errorMessage = null) }
            is RegistrationState.Event.UsernameChanged -> setState { copy(username = event.username, error = null, errorMessage = null) }
            is RegistrationState.Event.PasswordChanged -> setState { copy(password = event.password, error = null, errorMessage = null) }
            RegistrationState.Event.RegisterSelected -> register()
            is RegistrationState.Event.CaptchaSolved -> {
                setState { copy(error = null, errorMessage = null) }
                if (event.token.isBlank()) {
                    setState { copy(error = AccountUiError.CAPTCHA_RESPONSE_EMPTY) }
                } else {
                    register(captchaToken = event.token)
                }
            }
            RegistrationState.Event.CaptchaExpired -> setState {
                copy(
                    isLoading = false,
                    captchaChallengeId = currentState.captchaChallengeId + 1,
                    error = AccountUiError.CAPTCHA_EXPIRED,
                )
            }

            is RegistrationState.Event.CaptchaFailed -> setState {
                copy(
                    isLoading = false,
                    captchaChallengeId = currentState.captchaChallengeId + 1,
                    error = AccountUiError.CAPTCHA_LOAD_FAILED,
                )
            }
        }
    }

    private fun register(captchaToken: String? = null) {
        val email = currentState.email
        val username = currentState.username
        val password = currentState.password

        validateRegistrationForm(email, username, password)?.let { error ->
            setState { copy(error = error, errorMessage = null) }
            return
        }

        viewModelScope.launch {
            val isCaptchaAttempt = captchaToken != null
            setState {
                copy(
                    isLoading = true,
                    error = null,
                    errorMessage = null,
                    // Капчу не размонтируем, пока проверяем её же токен.
                    isCaptchaRequired = if (isCaptchaAttempt) isCaptchaRequired else false,
                    captchaChallengeId = if (isCaptchaAttempt) {
                        captchaChallengeId
                    } else {
                        currentState.captchaChallengeId + 1
                    },
                )
            }
            runSuspendCatching {
                registerUserUseCase(
                    UserRegistration(
                        email = email,
                        username = username,
                        password = password,
                        captchaResponse = captchaToken,
                    ),
                )
            }.fold(
                onSuccess = {
                    setState { copy(isLoading = false, isSuccess = true, errorMessage = null) }
                },
                onFailure = { error ->
                    if (error is AccountCaptchaRequiredException) {
                        setState {
                            copy(
                                isLoading = false,
                                isCaptchaRequired = true,
                                captchaChallengeId = currentState.captchaChallengeId + 1,
                                error = if (captchaToken != null) AccountUiError.CAPTCHA_REJECTED else null,
                                errorMessage = null,
                            )
                        }
                        setEffect(RegistrationState.Effect.HideKeyboard)
                        setEffect(RegistrationState.Effect.ShowCaptchaHint)
                    } else if (error is RegistrationException) {
                        setEffect(RegistrationState.Effect.DiscardAutofill)
                        setState {
                            copy(
                                isLoading = false,
                                error = AccountUiError.REGISTRATION_FAILED,
                                errorMessage = error.message,
                            )
                        }
                    } else {
                        setEffect(RegistrationState.Effect.DiscardAutofill)
                        setState {
                            copy(
                                isLoading = false,
                                error = AccountUiError.REGISTRATION_FAILED,
                                errorMessage = null,
                            )
                        }
                    }
                },
            )
        }
    }
}
