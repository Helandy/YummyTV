package su.afk.yummy.tv.feature.bloggers.list

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.bloggers.model.Blogger
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoCategory
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoSort

object BloggerVideosListState {
    data class State(
        val animeId: Int? = null,
        val videos: List<BloggerVideo> = emptyList(),
        val categories: List<BloggerVideoCategory> = emptyList(),
        val bloggers: List<Blogger> = emptyList(),
        val selectedCategory: String = "all",
        val selectedBloggerId: Int? = null,
        val sort: BloggerVideoSort = BloggerVideoSort.NEW,
        val isLoading: Boolean = true,
        val error: String? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetrySelected : Event
        data class VideoSelected(val videoId: Int) : Event
        data class BloggerDetailsSelected(val bloggerId: Int) : Event
        data class CategorySelected(val id: String) : Event
        data class BloggerSelected(val id: Int?) : Event
        data class SortSelected(val sort: BloggerVideoSort) : Event
        data object FiltersReset : Event
    }

    sealed interface Effect : UiEffect
}
