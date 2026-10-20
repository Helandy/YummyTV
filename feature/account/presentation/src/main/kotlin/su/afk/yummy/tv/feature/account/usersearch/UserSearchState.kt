package su.afk.yummy.tv.feature.account.usersearch

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.account.model.UserSearchItem

class UserSearchState {
    data class State(
        val query: String = "",
        val results: Flow<PagingData<UserSearchItem>> = flowOf(PagingData.empty()),
        val isSearchActive: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data class QueryChanged(val query: String) : Event
        data object SearchSubmitted : Event
        data class UserSelected(val nickname: String, val userId: Int = 0) : Event
    }

    sealed interface Effect : UiEffect
}
