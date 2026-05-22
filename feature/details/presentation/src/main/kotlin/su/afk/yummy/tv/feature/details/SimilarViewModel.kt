package su.afk.yummy.tv.feature.details

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
import su.afk.yummy.tv.domain.anime.GetAnimePreviewUseCase
import su.afk.yummy.tv.domain.anime.GetAnimeRecommendationsUseCase

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
            SimilarState.Event.SourceToggled -> {
                setState { copy(fromAi = !fromAi, similarState = SimilarUiState.Loading) }
                viewModelScope.launch { load() }
            }
        }
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

    private suspend fun load() {
        setState { copy(similarState = SimilarUiState.Loading) }
        runCatching { getAnimeRecommendations(animeId, currentState.fromAi) }.fold(
            onSuccess = { items ->
                setState {
                    copy(similarState = if (items.isEmpty()) SimilarUiState.Empty else SimilarUiState.Content(items))
                }
            },
            onFailure = { setState { copy(similarState = SimilarUiState.Empty) } },
        )
    }
}
