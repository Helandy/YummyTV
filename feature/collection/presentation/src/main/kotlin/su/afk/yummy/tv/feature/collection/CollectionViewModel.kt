package su.afk.yummy.tv.feature.collection

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.collection.usecase.GetCollectionUseCase
import su.afk.yummy.tv.feature.collection.handler.AnimePreviewFocusHandler
import su.afk.yummy.tv.feature.collection.presentation.R
import su.afk.yummy.tv.feature.details.IDetailsNavigator

@HiltViewModel(assistedFactory = CollectionViewModel.Factory::class)
class CollectionViewModel @AssistedInject internal constructor(
    @Assisted private val collectionId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getCollection: GetCollectionUseCase,
    private val stringProvider: StringProvider,
    private val animePreviewFocusHandler: AnimePreviewFocusHandler,
) : BaseViewModelNew<CollectionState.State, CollectionState.Event, CollectionState.Effect>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(collectionId: Int): CollectionViewModel
    }

    override fun createInitialState() = CollectionState.State()

    init {
        load()
    }

    override fun onEvent(event: CollectionState.Event) {
        when (event) {
            CollectionState.Event.BackSelected -> nav.back()
            CollectionState.Event.RetrySelected -> load()
            is CollectionState.Event.AnimeSelected -> {
                setState {
                    copy(
                        focusedItemId = event.animeId,
                        restoreFocusedItemOnEnter = true,
                    )
                }
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            }
            is CollectionState.Event.ItemFocused -> onItemFocused(event.animeId)
            is CollectionState.Event.GridScrolled -> setState {
                copy(firstVisibleItemIndex = event.index, firstVisibleItemScrollOffset = event.offset)
            }
            CollectionState.Event.FocusedItemRestoreHandled -> {
                if (currentState.restoreFocusedItemOnEnter) {
                    setState { copy(restoreFocusedItemOnEnter = false) }
                }
            }
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

    private fun load() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            runCatching { getCollection(collectionId) }.fold(
                onSuccess = { collection -> setState { copy(isLoading = false, collection = collection) } },
                onFailure = { e ->
                    setState { copy(isLoading = false, error = e.message ?: stringProvider.get(R.string.collection_load_error)) }
                },
            )
        }
    }
}
