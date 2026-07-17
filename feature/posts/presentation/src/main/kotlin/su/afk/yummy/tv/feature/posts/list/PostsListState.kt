package su.afk.yummy.tv.feature.posts.list

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.posts.model.PostCategory
import su.afk.yummy.tv.domain.posts.model.PostSort
import su.afk.yummy.tv.domain.posts.model.PostSummary

object PostsListState {
    data class State(
        val posts: Flow<PagingData<PostSummary>> = flowOf(PagingData.empty()),
        val categories: List<PostCategory> = emptyList(),
        val selectedCategory: String? = null,
        val sort: PostSort = PostSort.NEW,
        val categoriesLoading: Boolean = true,
    ) : UiState

    sealed interface Event : UiEvent {
        data class PostSelected(val postId: Int) : Event
        data class CategorySelected(val uri: String?) : Event
        data class SortSelected(val sort: PostSort) : Event
    }

    sealed interface Effect : UiEffect
}
