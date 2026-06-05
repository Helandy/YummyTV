package su.afk.yummy.tv.feature.top100

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
import su.afk.yummy.tv.domain.anime.usecase.GetAnimePreviewUseCase
import su.afk.yummy.tv.domain.top100.model.AnimeTopPage
import su.afk.yummy.tv.domain.top100.model.AnimeTopType
import su.afk.yummy.tv.domain.top100.usecase.GetAnimeTopUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.top100.presentation.R
import javax.inject.Inject

@HiltViewModel
class Top100ViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getAnimeTop: GetAnimeTopUseCase,
    private val getAnimePreview: GetAnimePreviewUseCase,
    private val stringProvider: StringProvider,
) : BaseViewModelNew<Top100State.State, Top100State.Event, Top100State.Effect>(savedStateHandle) {

    override fun createInitialState() = Top100State.State()

    private companion object {
        const val PAGE_SIZE = 100
    }

    private var previewJob: Job? = null

    init {
        load(AnimeTopType.TV, offset = 0, replace = true)
    }

    override fun onEvent(event: Top100State.Event) {
        when (event) {
            is Top100State.Event.TypeSelected -> {
                if (event.type != currentState.selectedType) {
                    setState { copy(selectedType = event.type, items = emptyList(), offset = 0, canLoadMore = true) }
                    load(event.type, offset = 0, replace = true)
                }
            }
            is Top100State.Event.AnimeSelected -> {
                setState { copy(focusedItemId = event.animeId, restoreFocusedItemOnEnter = true) }
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            }
            is Top100State.Event.ItemFocused -> onItemFocused(event.animeId)
            Top100State.Event.FocusedItemRestoreHandled -> {
                if (currentState.restoreFocusedItemOnEnter) {
                    setState { copy(restoreFocusedItemOnEnter = false) }
                }
            }
            Top100State.Event.LoadMore -> {
                val s = currentState
                if (!s.isLoadingMore && !s.isLoading && s.canLoadMore) {
                    load(s.selectedType, offset = s.offset, replace = false)
                }
            }
            Top100State.Event.RetrySelected -> load(currentState.selectedType, offset = 0, replace = true)
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

    private fun load(type: AnimeTopType, offset: Int, replace: Boolean) {
        viewModelScope.launch {
            if (replace) {
                setState { copy(isLoading = true, error = null) }
            } else {
                setState { copy(isLoadingMore = true) }
            }
            runCatching { loadVisiblePage(type, offset) }.fold(
                onSuccess = { page ->
                    setState {
                        copy(
                            isLoading = false,
                            isLoadingMore = false,
                            items = if (replace) page.items else items + page.items,
                            offset = page.nextOffset,
                            canLoadMore = page.canLoadMore,
                        )
                    }
                },
                onFailure = { e ->
                    setState {
                        copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = if (replace) e.message ?: stringProvider.get(R.string.top100_load_error) else error,
                        )
                    }
                },
            )
        }
    }

    private suspend fun loadVisiblePage(
        type: AnimeTopType,
        offset: Int,
    ): AnimeTopPage {
        var page = getAnimeTop(type, PAGE_SIZE, offset)
        while (page.items.isEmpty() && page.canLoadMore && page.nextOffset > offset) {
            val nextPage = getAnimeTop(type, PAGE_SIZE, page.nextOffset)
            page = page.copy(
                items = nextPage.items,
                nextOffset = nextPage.nextOffset,
                canLoadMore = nextPage.canLoadMore,
            )
        }
        return page
    }
}
