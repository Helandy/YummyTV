package su.afk.yummy.tv.feature.posts.list

import androidx.lifecycle.SavedStateHandle
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.utils.OffsetPage
import su.afk.yummy.tv.core.utils.OffsetPagingSource
import su.afk.yummy.tv.domain.posts.model.PostSort
import su.afk.yummy.tv.domain.posts.usecase.PostsUseCases
import su.afk.yummy.tv.feature.posts.IPostsNavigator
import javax.inject.Inject

@HiltViewModel
class PostsListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val navigator: IPostsNavigator,
    private val posts: PostsUseCases,
) : BaseViewModelNew<PostsListState.State, PostsListState.Event, PostsListState.Effect>(
    savedStateHandle
) {
    override fun createInitialState() = PostsListState.State(posts = createFlow(null, PostSort.NEW))

    init {
        loadCategories()
    }

    override fun onEvent(event: PostsListState.Event) {
        when (event) {
            is PostsListState.Event.PostSelected -> nav.navigate(navigator.details(event.postId))
            is PostsListState.Event.CategorySelected -> if (event.uri != currentState.selectedCategory) {
                setState { copy(selectedCategory = event.uri, posts = createFlow(event.uri, sort)) }
            }

            is PostsListState.Event.SortSelected -> if (event.sort != currentState.sort) {
                setState {
                    copy(
                        sort = event.sort,
                        posts = createFlow(selectedCategory, event.sort)
                    )
                }
            }
        }
    }

    private fun loadCategories() = viewModelScope.launch {
        runCatching { posts.categories() }.fold(
            { loaded -> setState { copy(categories = loaded, categoriesLoading = false) } },
            { setState { copy(categoriesLoading = false) } },
        )
    }

    private fun createFlow(category: String?, sort: PostSort) =
        Pager(PagingConfig(pageSize = 20, initialLoadSize = 20, enablePlaceholders = false)) {
            OffsetPagingSource { limit, offset ->
                val page = posts.page(category, sort.apiValue, limit.coerceAtMost(20), offset)
                OffsetPage(page, offset + page.size, page.size >= limit.coerceAtMost(20))
            }
        }.flow.cachedIn(viewModelScope)
}
