package su.afk.yummy.tv.feature.account.userprofile

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.account.usecase.GetUserProfileByNicknameUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator

@HiltViewModel(assistedFactory = UserProfileResolverViewModel.Factory::class)
class UserProfileResolverViewModel @AssistedInject constructor(
    @Assisted private val nickname: String,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val accountNavigator: IAccountNavigator,
    private val getProfile: GetUserProfileByNicknameUseCase,
) : BaseViewModel<UserProfileResolverState.State, UserProfileResolverState.Event, UserProfileResolverState.Effect>() {
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
        runSuspendCatching { getProfile(nickname) }
            .onSuccess { profile ->
                if (profile.userId > 0) nav.replace(accountNavigator.getUserProfileDest(profile.userId))
                else setState { copy(isLoading = false, hasError = true) }
            }
            .onFailure { setState { copy(isLoading = false, hasError = true) } }
    }
}
