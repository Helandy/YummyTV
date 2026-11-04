package su.afk.yummy.tv.feature.account.friends

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.account.model.UserFriend

class FriendsState {
    data class State(
        val friends: Flow<PagingData<UserFriend>> = flowOf(PagingData.empty()),
        /** Открыт список друзей самого пользователя: только тогда доступны его диалоги. */
        val isOwnList: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object UserSearchSelected : Event
        data object MessagesSelected : Event
        data class FriendSelected(val userId: Int) : Event
    }

    sealed interface Effect : UiEffect
}
