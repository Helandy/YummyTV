package su.afk.yummy.tv.feature.collection.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.utils.OffsetPage
import su.afk.yummy.tv.core.utils.OffsetPagingSource
import su.afk.yummy.tv.domain.collection.usecase.GetCollectionsUseCase
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
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
) : BaseViewModelNew<CollectionsCatalogState.State, CollectionsCatalogState.Event, CollectionsCatalogState.Effect>(
    savedStateHandle
) {

    override fun createInitialState() =
        CollectionsCatalogState.State(items = createPagingFlow())

    override fun onEvent(event: CollectionsCatalogState.Event) {
        when (event) {
            CollectionsCatalogState.Event.BackSelected -> nav.back()
            CollectionsCatalogState.Event.RetrySelected -> Unit
            is CollectionsCatalogState.Event.CollectionSelected ->
                nav.navigate(collectionNavigator.getCollectionDest(event.collectionId))
        }
    }

    private fun createPagingFlow() =
        Pager(
            config = PagingConfig(
                pageSize = COLLECTIONS_PAGE_SIZE,
                initialLoadSize = COLLECTIONS_PAGE_SIZE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                OffsetPagingSource { limit, offset ->
                    val page = getCollections(limit, offset)
                    OffsetPage(
                        items = page.items,
                        nextOffset = page.nextOffset,
                        canLoadMore = page.canLoadMore,
                    )
                }
            },
        ).flow.cachedIn(viewModelScope)
}
