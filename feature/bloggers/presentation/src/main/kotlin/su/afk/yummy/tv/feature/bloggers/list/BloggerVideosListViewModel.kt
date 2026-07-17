package su.afk.yummy.tv.feature.bloggers.list

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.utils.runSuspendCatching
import su.afk.yummy.tv.domain.bloggers.usecase.GetAnimeBloggerVideosUseCase
import su.afk.yummy.tv.domain.bloggers.usecase.GetBloggerVideosUseCase
import su.afk.yummy.tv.domain.bloggers.usecase.GetBloggersDirectoryUseCase
import su.afk.yummy.tv.feature.bloggers.IBloggerVideosNavigator
import su.afk.yummy.tv.feature.bloggers.presentation.R

@HiltViewModel(assistedFactory = BloggerVideosListViewModel.Factory::class)
class BloggerVideosListViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val stringProvider: StringProvider,
    private val getVideos: GetBloggerVideosUseCase,
    private val getAnimeVideos: GetAnimeBloggerVideosUseCase,
    private val getDirectory: GetBloggersDirectoryUseCase,
    private val bloggerNavigator: IBloggerVideosNavigator,
    @Assisted private val animeId: Int?,
) : BaseViewModelNew<BloggerVideosListState.State, BloggerVideosListState.Event, BloggerVideosListState.Effect>(
    savedStateHandle
) {
    override fun createInitialState() = BloggerVideosListState.State(animeId = animeId)

    init {
        load()
    }

    override fun onEvent(event: BloggerVideosListState.Event) {
        when (event) {
            BloggerVideosListState.Event.BackSelected -> nav.back()
            BloggerVideosListState.Event.RetrySelected -> load()
            is BloggerVideosListState.Event.VideoSelected -> nav.navigate(
                bloggerNavigator.video(
                    event.videoId
                )
            )

            is BloggerVideosListState.Event.BloggerDetailsSelected -> nav.navigate(
                bloggerNavigator.blogger(event.bloggerId)
            )

            is BloggerVideosListState.Event.CategorySelected -> {
                setState { copy(selectedCategory = event.id) }
                loadVideos()
            }

            is BloggerVideosListState.Event.BloggerSelected -> {
                setState { copy(selectedBloggerId = event.id) }
                loadVideos()
            }

            is BloggerVideosListState.Event.SortSelected -> {
                setState { copy(sort = event.sort) }
                loadVideos()
            }

            BloggerVideosListState.Event.FiltersReset -> {
                setState { copy(selectedCategory = "all", selectedBloggerId = null) }
                loadVideos()
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            runSuspendCatching {
                if (animeId != null) {
                    getAnimeVideos(animeId, limit = 30)
                } else {
                    val directory = async { getDirectory() }
                    val videos = async { getVideos(limit = 30) }
                    val result = directory.await()
                    setState { copy(categories = result.categories, bloggers = result.bloggers) }
                    videos.await()
                }
            }.fold(
                onSuccess = { setState { copy(videos = it, isLoading = false) } },
                onFailure = { error ->
                    setState {
                        copy(
                            isLoading = false,
                            error = error.message
                                ?: stringProvider.get(R.string.blogger_videos_load_error)
                        )
                    }
                },
            )
        }
    }

    private fun loadVideos() {
        if (animeId != null) return
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            runSuspendCatching {
                getVideos(
                    category = currentState.selectedCategory,
                    bloggerId = currentState.selectedBloggerId,
                    sort = currentState.sort,
                    limit = 30,
                )
            }.fold(
                onSuccess = { setState { copy(videos = it, isLoading = false) } },
                onFailure = { error ->
                    setState {
                        copy(
                            isLoading = false,
                            error = error.message
                                ?: stringProvider.get(R.string.blogger_videos_load_error)
                        )
                    }
                },
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int?): BloggerVideosListViewModel
    }
}
