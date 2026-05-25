package su.afk.yummy.tv.feature.collection

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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
import su.afk.yummy.tv.domain.collection.usecase.GetCollectionUseCase
import su.afk.yummy.tv.feature.collection.presentation.R
import su.afk.yummy.tv.feature.details.IDetailsNavigator

@HiltViewModel(assistedFactory = CollectionViewModel.Factory::class)
class CollectionViewModel @AssistedInject constructor(
    @Assisted private val collectionId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getCollection: GetCollectionUseCase,
    private val getAnimePreview: GetAnimePreviewUseCase,
    private val stringProvider: StringProvider,
) : BaseViewModelNew<CollectionState.State, CollectionState.Event, CollectionState.Effect>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(collectionId: Int): CollectionViewModel
    }

    override fun createInitialState() = CollectionState.State()

    private var previewJob: Job? = null

    init {
        load()
    }

    override fun onEvent(event: CollectionState.Event) {
        when (event) {
            CollectionState.Event.BackSelected -> nav.back()
            CollectionState.Event.RetrySelected -> load()
            is CollectionState.Event.AnimeSelected -> nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            is CollectionState.Event.ItemFocused -> onItemFocused(event.animeId)
            is CollectionState.Event.GridScrolled -> setState {
                copy(firstVisibleItemIndex = event.index, firstVisibleItemScrollOffset = event.offset)
            }
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
