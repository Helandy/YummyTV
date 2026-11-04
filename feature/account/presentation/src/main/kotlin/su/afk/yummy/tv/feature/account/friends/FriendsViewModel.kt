package su.afk.yummy.tv.feature.account.friends

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.paging.pagingFlow
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserFriendsUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.messages.IMessagesNavigator

private const val FRIENDS_PAGE_SIZE = 20

@HiltViewModel(assistedFactory = FriendsViewModel.Factory::class)
class FriendsViewModel @AssistedInject internal constructor(
    @Assisted private val userId: Int,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val accountNavigator: IAccountNavigator,
    private val getUserFriends: GetUserFriendsUseCase,
    private val getAccountSession: GetAccountSessionUseCase,
    private val messagesNavigator: IMessagesNavigator,
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

    init {
        viewModelScope.launch {
            val session = getAccountSession()
            setState { copy(isOwnList = session.isAuthorized && session.userId == userId) }
        }
    }

    override fun onEvent(event: FriendsState.Event) {
        when (event) {
            FriendsState.Event.BackSelected -> nav.back()
            FriendsState.Event.UserSearchSelected -> nav.navigate(accountNavigator.getUserSearchDest())
            FriendsState.Event.MessagesSelected -> if (currentState.isOwnList) nav.navigate(messagesNavigator.dialogs())
            is FriendsState.Event.FriendSelected ->
                if (event.userId > 0) nav.navigate(accountNavigator.getUserProfileDest(event.userId))
        }
    }
}
