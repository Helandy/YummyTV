package su.afk.yummy.tv.feature.account.friends

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.paging.pagingFlow
import su.afk.yummy.tv.domain.account.usecase.GetUserFriendsUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator

private const val FRIENDS_PAGE_SIZE = 20

@HiltViewModel(assistedFactory = FriendsViewModel.Factory::class)
class FriendsViewModel @AssistedInject internal constructor(
    @Assisted private val userId: Int,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val accountNavigator: IAccountNavigator,
    private val getUserFriends: GetUserFriendsUseCase,
) : BaseViewModel<FriendsState.State, FriendsState.Event, FriendsState.Effect>() {

    @AssistedFactory
    interface Factory {
        fun create(userId: Int): FriendsViewModel
    }

    override fun createInitialState() = FriendsState.State(
        friends = pagingFlow(viewModelScope, pageSize = FRIENDS_PAGE_SIZE, itemKey = { it.id }) { limit, offset ->
            getUserFriends(userId, limit, offset)
        },
    )

    override fun onEvent(event: FriendsState.Event) {
        when (event) {
            FriendsState.Event.BackSelected -> nav.back()
            FriendsState.Event.UserSearchSelected -> nav.navigate(accountNavigator.getUserSearchDest())
            is FriendsState.Event.FriendSelected ->
                if (event.userId > 0) nav.navigate(accountNavigator.getUserProfileDest(event.userId))
        }
    }
}
