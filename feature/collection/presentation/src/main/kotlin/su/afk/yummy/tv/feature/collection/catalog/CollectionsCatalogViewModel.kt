package su.afk.yummy.tv.feature.collection.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.utils.OffsetPage
import su.afk.yummy.tv.core.utils.OffsetPagingSource
import su.afk.yummy.tv.core.utils.runSuspendCatching
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.collection.CollectionMutationNotifier
import su.afk.yummy.tv.domain.collection.model.CreateCollectionRequest
import su.afk.yummy.tv.domain.collection.usecase.CreateCollectionUseCase
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
    private val mutationNotifier: CollectionMutationNotifier,
    private val createCollection: CreateCollectionUseCase,
    private val getAccountSession: GetAccountSessionUseCase,
    private val stringProvider: StringProvider,
) : BaseViewModelNew<CollectionsCatalogState.State, CollectionsCatalogState.Event, CollectionsCatalogState.Effect>(
    savedStateHandle
) {

    override fun createInitialState() =
        CollectionsCatalogState.State(items = createPagingFlow())

    override fun onEvent(event: CollectionsCatalogState.Event) {
        when (event) {
            CollectionsCatalogState.Event.BackSelected -> nav.back()
            CollectionsCatalogState.Event.RetrySelected -> Unit
            CollectionsCatalogState.Event.CreateSelected -> openCreateDialog()
            CollectionsCatalogState.Event.CreateDismissed -> {
                if (!currentState.isCreating) {
                    setState { copy(isCreateDialogVisible = false) }
                }
            }

            CollectionsCatalogState.Event.CreateConfirmed -> createCollection()
            is CollectionsCatalogState.Event.CreateTitleChanged ->
                setState { copy(createTitle = event.title) }

            is CollectionsCatalogState.Event.CreateDescriptionChanged ->
                setState { copy(createDescription = event.description) }

            is CollectionsCatalogState.Event.CreatePublicChanged ->
                setState { copy(isCreatePublic = event.isPublic) }

            is CollectionsCatalogState.Event.CollectionSelected ->
                nav.navigate(collectionNavigator.getCollectionDest(event.collectionId))
        }
    }

    private fun openCreateDialog() {
        viewModelScope.launch {
            val session = getAccountSession()
            if (!session.isAuthorized || session.userId <= 0) {
                setEffect(
                    CollectionsCatalogState.Effect.ShowToast(
                        stringProvider.get(R.string.collection_create_auth_required)
                    )
                )
                return@launch
            }
            setState {
                copy(
                    isCreateDialogVisible = true,
                    createTitle = "",
                    createDescription = "",
                    isCreatePublic = true,
                )
            }
        }
    }

    private fun createCollection() {
        val title = currentState.createTitle.trim()
        if (title.isEmpty() || currentState.isCreating) return
        val request = CreateCollectionRequest(
            title = title,
            description = currentState.createDescription.trim(),
            isPublic = currentState.isCreatePublic,
        )
        viewModelScope.launch {
            setState { copy(isCreating = true) }
            runSuspendCatching { createCollection(request) }.fold(
                onSuccess = { collectionId ->
                    setState {
                        copy(
                            isCreating = false,
                            isCreateDialogVisible = false,
                        )
                    }
                    nav.navigate(collectionNavigator.getCollectionDest(collectionId))
                },
                onFailure = {
                    setState { copy(isCreating = false) }
                    setEffect(
                        CollectionsCatalogState.Effect.ShowToast(
                            stringProvider.get(R.string.collection_create_error)
                        )
                    )
                },
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createPagingFlow() =
        mutationNotifier.version
            .flatMapLatest { createPagerFlow() }
            .cachedIn(viewModelScope)

    private fun createPagerFlow() =
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
        ).flow
}
