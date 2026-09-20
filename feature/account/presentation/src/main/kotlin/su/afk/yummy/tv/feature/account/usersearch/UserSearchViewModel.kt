package su.afk.yummy.tv.feature.account.usersearch

import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.paging.pagingFlow
import su.afk.yummy.tv.domain.account.usecase.SearchUsersUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class UserSearchViewModel @Inject constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val accountNavigator: IAccountNavigator,
    private val searchUsers: SearchUsersUseCase,
) : BaseViewModel<UserSearchState.State, UserSearchState.Event, UserSearchState.Effect>() {
    override fun createInitialState() = UserSearchState.State()

    private var searchJob: Job? = null

    override fun onEvent(event: UserSearchState.Event) {
        when (event) {
            UserSearchState.Event.BackSelected -> nav.back()
            is UserSearchState.Event.QueryChanged -> updateQuery(event.query)
            UserSearchState.Event.SearchSubmitted -> submitSearch()
            is UserSearchState.Event.UserSelected -> {
                val nickname = event.nickname.trim()
                if (nickname.isNotEmpty()) {
                    nav.navigate(accountNavigator.getUserProfileByNicknameDest(nickname))
                }
            }
        }
    }

    private fun updateQuery(query: String) {
        searchJob?.cancel()
        setState {
            copy(query = query, results = flowOf(PagingData.empty()), isSearchActive = false)
        }
        val normalized = query.trim()
        if (normalized.length < MIN_QUERY_LENGTH) return
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE)
            setResults(normalized)
        }
    }

    private fun submitSearch() {
        searchJob?.cancel()
        val query = currentState.query.trim()
        if (query.length < MIN_QUERY_LENGTH) return
        setState { copy(query = query) }
        setResults(query)
    }

    private fun setResults(query: String) {
        val flow = pagingFlow(viewModelScope, pageSize = PAGE_SIZE, itemKey = { it.id }) { limit, offset ->
            searchUsers(query, limit, offset)
        }
        setState { copy(results = flow, isSearchActive = true) }
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 2
        const val PAGE_SIZE = 20
        val SEARCH_DEBOUNCE = 500.milliseconds
    }
}
