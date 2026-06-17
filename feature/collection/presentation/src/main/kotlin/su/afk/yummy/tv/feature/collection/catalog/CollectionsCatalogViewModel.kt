package su.afk.yummy.tv.feature.collection.catalog

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.collection.usecase.GetCollectionsUseCase
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.collection.presentation.R
import javax.inject.Inject

private const val COLLECTIONS_PAGE_SIZE = 20

@HiltViewModel
class CollectionsCatalogViewModel @Inject internal constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val collectionNavigator: ICollectionNavigator,
    private val getCollections: GetCollectionsUseCase,
    private val stringProvider: StringProvider,
) : BaseViewModelNew<CollectionsCatalogState.State, CollectionsCatalogState.Event, CollectionsCatalogState.Effect>(
    savedStateHandle
) {

    override fun createInitialState() = CollectionsCatalogState.State()

    init {
        load(replace = true)
    }

    override fun onEvent(event: CollectionsCatalogState.Event) {
        when (event) {
            CollectionsCatalogState.Event.BackSelected -> nav.back()
            CollectionsCatalogState.Event.RetrySelected -> load(replace = true)
            CollectionsCatalogState.Event.LoadMoreSelected -> loadMore()
            is CollectionsCatalogState.Event.CollectionSelected ->
                nav.navigate(collectionNavigator.getCollectionDest(event.collectionId))
        }
    }

    private fun loadMore() {
        val state = currentState
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore) return
        load(replace = false)
    }

    private fun load(replace: Boolean) {
        viewModelScope.launch {
            val offset = if (replace) 0 else currentState.offset
            setState {
                if (replace) {
                    copy(isLoading = true, isLoadingMore = false, error = null)
                } else {
                    copy(isLoadingMore = true, error = null)
                }
            }
            runCatching { getCollections(COLLECTIONS_PAGE_SIZE, offset) }.fold(
                onSuccess = { page ->
                    setState {
                        copy(
                            items = if (replace) page.items else items + page.items,
                            isLoading = false,
                            isLoadingMore = false,
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
                            error = e.message
                                ?: stringProvider.get(R.string.collection_catalog_load_error),
                        )
                    }
                },
            )
        }
    }
}
