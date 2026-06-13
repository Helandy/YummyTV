package su.afk.yummy.tv.feature.top

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.top.model.AnimeTopPage
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import su.afk.yummy.tv.domain.top.usecase.GetAnimeTopUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.top.presentation.R
import javax.inject.Inject

@HiltViewModel
class TopViewModel @Inject internal constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getAnimeTop: GetAnimeTopUseCase,
    private val stringProvider: StringProvider,
    private val animePreviewFocusHandler: AnimePreviewFocusHandler,
) : BaseViewModelNew<TopState.State, TopState.Event, TopState.Effect>(savedStateHandle) {

    override fun createInitialState() = TopState.State()

    private companion object {
        const val PAGE_SIZE = 100
    }

    init {
        load(AnimeTopType.TV, offset = 0, replace = true)
    }

    override fun onEvent(event: TopState.Event) {
        when (event) {
            is TopState.Event.TypeSelected -> {
                if (event.type != currentState.selectedType) {
                    setState {
                        copy(
                            selectedType = event.type,
                            items = emptyList(),
                            offset = 0,
                            canLoadMore = true
                        )
                    }
                    load(event.type, offset = 0, replace = true)
                }
            }

            is TopState.Event.AnimeSelected -> {
                setState { copy(focusedItemId = event.animeId, restoreFocusedItemOnEnter = true) }
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            }

            is TopState.Event.ItemFocused -> onItemFocused(event.animeId)
            TopState.Event.FocusedItemRestoreHandled -> {
                if (currentState.restoreFocusedItemOnEnter) {
                    setState { copy(restoreFocusedItemOnEnter = false) }
                }
            }

            TopState.Event.LoadMore -> {
                val s = currentState
                if (!s.isLoadingMore && !s.isLoading && s.canLoadMore) {
                    load(s.selectedType, offset = s.offset, replace = false)
                }
            }

            TopState.Event.RetrySelected -> load(
                currentState.selectedType,
                offset = 0,
                replace = true
            )
        }
    }

    private fun onItemFocused(animeId: Int) {
        if (currentState.focusedItemId == animeId) return
        setState { copy(focusedItemId = animeId, focusedPreview = null) }
        animePreviewFocusHandler.focus(
            scope = viewModelScope,
            animeId = animeId,
            isCurrentFocus = { currentState.focusedItemId == animeId },
            onCachedPreview = { preview, _ -> setState { copy(focusedPreview = preview) } },
            onLoadedPreview = { result ->
                if (result.isCurrentFocus) {
                    setState { copy(focusedPreview = result.preview) }
                }
            }
        )
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
                        if (selectedType == type) {
                            copy(
                                isLoading = false,
                                isLoadingMore = false,
                                items = if (replace) page.items else items + page.items,
                                offset = page.nextOffset,
                                canLoadMore = page.canLoadMore,
                            )
                        } else {
                            this
                        }
                    }
                },
                onFailure = { e ->
                    setState {
                        if (selectedType == type) {
                            copy(
                                isLoading = false,
                                isLoadingMore = false,
                                error = if (replace) {
                                    e.message ?: stringProvider.get(R.string.top_load_error)
                                } else {
                                    error
                                },
                            )
                        } else {
                            this
                        }
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
