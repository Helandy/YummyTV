package su.afk.yummy.tv.feature.search

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.anime.GetAnimePreviewUseCase
import su.afk.yummy.tv.domain.search.SearchUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.search.presentation.R
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val search: SearchUseCase,
    private val getAnimePreview: GetAnimePreviewUseCase,
    private val stringProvider: StringProvider,
) : BaseViewModelNew<SearchState.State, SearchState.Event, SearchState.Effect>(savedStateHandle) {

    override fun createInitialState() = SearchState.State()

    private var searchJob: Job? = null
    private var previewJob: Job? = null

    private companion object {
        const val PAGE_SIZE = 10
        const val DEBOUNCE_MS = 3_000L
    }

    override fun onEvent(event: SearchState.Event) {
        when (event) {
            is SearchState.Event.QueryChanged -> onQueryChanged(event.query)
            is SearchState.Event.ItemSelected -> nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            is SearchState.Event.ItemFocused -> onItemFocused(event.animeId)
            SearchState.Event.SearchSubmitted -> onSearchSubmitted()
            SearchState.Event.LoadMore -> loadMore()
        }
    }

    private fun onItemFocused(animeId: Int) {
        if (currentState.focusedItemId == animeId) return
        previewJob?.cancel()
        setState { copy(focusedItemId = animeId, focusedPreview = null) }
        previewJob = viewModelScope.launch {
            delay(600)
            runCatching { getAnimePreview(animeId) }.onSuccess { preview ->
                setState { copy(focusedPreview = preview) }
            }
        }
    }

    private fun onQueryChanged(query: String) {
        setState { copy(query = query, items = emptyList(), offset = 0, error = null, canLoadMore = false) }
        searchJob?.cancel()
        if (query.isBlank()) {
            setState { copy(isLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            load(query, offset = 0, replace = true)
        }
    }

    private fun onSearchSubmitted() {
        val query = currentState.query
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch { load(query, offset = 0, replace = true) }
    }

    private fun loadMore() {
        val s = currentState
        if (!s.isLoading && s.canLoadMore && s.query.isNotBlank()) {
            viewModelScope.launch { load(s.query, offset = s.offset, replace = false) }
        }
    }

    private suspend fun load(query: String, offset: Int, replace: Boolean) {
        setState { if (replace) copy(isLoading = true, error = null) else copy(isLoading = true) }
        runCatching { search(query, PAGE_SIZE, offset) }.fold(
            onSuccess = { newItems ->
                setState {
                    copy(
                        isLoading = false,
                        items = if (replace) newItems else items + newItems,
                        offset = offset + newItems.size,
                        canLoadMore = newItems.size >= PAGE_SIZE,
                    )
                }
            },
            onFailure = { e ->
                setState { copy(isLoading = false, error = e.message ?: stringProvider.get(R.string.search_error)) }
            },
        )
    }
}
