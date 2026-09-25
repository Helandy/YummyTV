package su.afk.yummy.tv.feature.posts.list

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.utils.paging.pagingFlow
import su.afk.yummy.tv.domain.posts.model.PostSort
import su.afk.yummy.tv.domain.posts.usecase.GetPostCategoriesUseCase
import su.afk.yummy.tv.domain.posts.usecase.GetPostsUseCase
import su.afk.yummy.tv.feature.posts.IPostsNavigator
import javax.inject.Inject

@HiltViewModel
class PostsListViewModel @Inject constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val navigator: IPostsNavigator,
    private val getPostCategories: GetPostCategoriesUseCase,
    private val getPosts: GetPostsUseCase,
) : BaseViewModel<PostsListState.State, PostsListState.Event, PostsListState.Effect>() {
    override fun createInitialState() = PostsListState.State(posts = createFlow(null, PostSort.NEW))

    init {
        loadCategories()
    }

    override fun onEvent(event: PostsListState.Event) {
        when (event) {
            is PostsListState.Event.PostSelected -> nav.navigateDetail(navigator.details(event.postId))
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
        runSuspendCatching { getPostCategories() }.fold(
            { loaded ->
                setState {
                    copy(
                        categories = loaded.toImmutableList(),
                        categoriesLoading = false
                    )
                }
            },
            { setState { copy(categoriesLoading = false) } },
        )
    }

    private fun createFlow(category: String?, sort: PostSort) =
        pagingFlow(viewModelScope, itemKey = { it.id }) { limit, offset ->
            getPosts(category, sort.apiValue, limit, offset)
        }
}
