package su.afk.yummy.tv.feature.account.userprofile

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.account.usecase.GetUserProfileByNicknameUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator

@HiltViewModel(assistedFactory = UserProfileResolverViewModel.Factory::class)
class UserProfileResolverViewModel @AssistedInject constructor(
    @Assisted private val nickname: String,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val accountNavigator: IAccountNavigator,
    private val getProfile: GetUserProfileByNicknameUseCase,
) : BaseViewModelNew<UserProfileResolverState.State, UserProfileResolverState.Event, UserProfileResolverState.Effect>(
    savedStateHandle
) {
    @AssistedFactory
    interface Factory {
        fun create(nickname: String): UserProfileResolverViewModel
    }

    override fun createInitialState() = UserProfileResolverState.State()

    init {
        resolve()
    }

    override fun onEvent(event: UserProfileResolverState.Event) {
        when (event) {
            UserProfileResolverState.Event.BackSelected -> nav.back()
            UserProfileResolverState.Event.RetrySelected -> resolve()
        }
    }

    private fun resolve() = viewModelScope.launch {
        setState { copy(isLoading = true, hasError = false) }
        runCatching { getProfile(nickname) }
            .onSuccess { profile ->
                if (profile.userId > 0) nav.replace(accountNavigator.getUserProfileDest(profile.userId))
                else setState { copy(isLoading = false, hasError = true) }
            }
            .onFailure { setState { copy(isLoading = false, hasError = true) } }
    }
}
