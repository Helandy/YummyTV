package su.afk.yummy.tv.feature.details.similar

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
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.anime.usecase.GetAnimePreviewUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeRecommendationsUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.details.details.SimilarUiState

@HiltViewModel(assistedFactory = SimilarViewModel.Factory::class)
class SimilarViewModel @AssistedInject constructor(
    @Assisted private val animeId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getAnimeRecommendations: GetAnimeRecommendationsUseCase,
    private val getAnimePreview: GetAnimePreviewUseCase,
) : BaseViewModelNew<SimilarState.State, SimilarState.Event, SimilarState.Effect>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): SimilarViewModel
    }

    override fun createInitialState() = SimilarState.State()

    private var previewJob: Job? = null

    init {
        viewModelScope.launch { load() }
    }

    override fun onEvent(event: SimilarState.Event) {
        when (event) {
            SimilarState.Event.BackSelected -> nav.back()
            is SimilarState.Event.AnimeSelected ->
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            is SimilarState.Event.ItemFocused -> onItemFocused(event.animeId)
            is SimilarState.Event.SourceSelected -> selectSource(event.fromAi)
            SimilarState.Event.SourceToggled -> selectSource(!currentState.fromAi)
        }
    }

    private fun selectSource(fromAi: Boolean) {
        if (currentState.fromAi == fromAi) return
        setState { copy(fromAi = fromAi) }
        viewModelScope.launch { load(fromAi) }
    }

    private fun onItemFocused(id: Int) {
        if (currentState.focusedItemId == id) return
        previewJob?.cancel()
        setState { copy(focusedItemId = id, focusedPreview = null) }
        previewJob = viewModelScope.launch {
            delay(600)
            runCatching { getAnimePreview(id) }.onSuccess { preview ->
                setState { copy(focusedPreview = preview) }
            }
        }
    }

    private suspend fun load(fromAi: Boolean = currentState.fromAi) {
        setState { copy(similarState = SimilarUiState.Loading) }
        runCatching { getAnimeRecommendations(animeId, fromAi) }.fold(
            onSuccess = { items ->
                setState {
                    if (this.fromAi == fromAi) {
                        val nextState = if (items.isEmpty()) {
                            SimilarUiState.Empty
                        } else {
                            SimilarUiState.Content(items)
                        }
                        copy(similarState = nextState)
                    } else {
                        this
                    }
                }
            },
            onFailure = {
                setState {
                    if (this.fromAi == fromAi) copy(similarState = SimilarUiState.Empty) else this
                }
            },
        )
    }
}
