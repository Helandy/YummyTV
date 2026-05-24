package su.afk.yummy.tv.feature.account

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.navigation.TopBarFocusTarget
import su.afk.yummy.tv.core.storage.settings.SettingsStore
import su.afk.yummy.tv.domain.account.LoginUseCase
import su.afk.yummy.tv.domain.account.LogoutUseCase
import su.afk.yummy.tv.domain.account.RefreshAccountUseCase
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val settingsStore: SettingsStore,
    private val login: LoginUseCase,
    private val logout: LogoutUseCase,
    private val refreshAccount: RefreshAccountUseCase,
) : BaseViewModelNew<AccountState.State, AccountState.Event, AccountState.Effect>(savedStateHandle) {

    private companion object {
        const val TAG = "YaniAccount"
    }

    override fun createInitialState() = AccountState.State()

    init {
        settingsStore.yaniAccessToken
            .onEach { setState { copy(accessToken = it) } }
            .launchIn(viewModelScope)
        settingsStore.yaniUserId
            .onEach { setState { copy(userId = it) } }
            .launchIn(viewModelScope)
        settingsStore.yaniNickname
            .onEach { setState { copy(nickname = it) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: AccountState.Event) {
        when (event) {
            AccountState.Event.BackSelected -> {
                nav.requestTopBarFocus(TopBarFocusTarget.TRAILING_ACTION)
                nav.back()
            }
            is AccountState.Event.LoginChanged -> setState { copy(login = event.login, error = null) }
            is AccountState.Event.PasswordChanged -> setState { copy(password = event.password, error = null) }
            AccountState.Event.LoginSelected -> {
                Log.d(TAG, "LoginSelected loginBlank=${currentState.login.isBlank()} passwordBlank=${currentState.password.isBlank()}")
                login()
            }
            AccountState.Event.LogoutSelected -> viewModelScope.launch {
                setState { copy(isLoading = true, error = null) }
                runCatching { logout() }.fold(
                    onSuccess = {
                        setState { copy(isLoading = false, password = "") }
                        nav.requestTopBarFocus(TopBarFocusTarget.TRAILING_ACTION)
                        nav.back()
                    },
                    onFailure = { setState { copy(isLoading = false, error = it.message) } },
                )
            }
            AccountState.Event.RefreshProfileSelected -> viewModelScope.launch {
                setState { copy(isLoading = true, error = null) }
                runCatching { refreshAccount() }.fold(
                    onSuccess = { setState { copy(isLoading = false) } },
                    onFailure = { setState { copy(isLoading = false, error = it.message) } },
                )
            }
        }
    }

    private fun login() {
        val loginValue = currentState.login.trim()
        val passwordValue = currentState.password
        if (loginValue.isBlank() || passwordValue.isBlank()) {
            Log.d(TAG, "Login blocked by local validation")
            setState { copy(error = "Login and password are required") }
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "Calling /profile/login")
            setState { copy(isLoading = true, error = null) }
            runCatching { login(loginValue, passwordValue) }.fold(
                onSuccess = { account ->
                    Log.d(TAG, "Login succeeded userId=${account.id}")
                    setState {
                        copy(
                            isLoading = false,
                            password = "",
                            userId = account.id,
                            nickname = account.nickname,
                        )
                    }
                },
                onFailure = { error ->
                    Log.d(TAG, "Login failed: ${error::class.simpleName}: ${error.message}")
                    val message = if (error::class.simpleName == "YaniCaptchaRequiredException") {
                        "Captcha required. Login on the website first, then try again later."
                    } else {
                        error.message ?: "Could not sign in"
                    }
                    setState { copy(isLoading = false, error = message) }
                },
            )
        }
    }
}
