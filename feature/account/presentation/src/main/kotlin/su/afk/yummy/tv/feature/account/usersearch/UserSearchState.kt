package su.afk.yummy.tv.feature.account.usersearch

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
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
        data class UserSelected(val nickname: String) : Event
    }

    sealed interface Effect : UiEffect
}
