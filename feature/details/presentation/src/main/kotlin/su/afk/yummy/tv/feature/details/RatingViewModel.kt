package su.afk.yummy.tv.feature.details

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
import su.afk.yummy.tv.domain.account.DeleteAnimeRatingUseCase
import su.afk.yummy.tv.domain.account.GetAnimeListStatsUseCase
import su.afk.yummy.tv.domain.account.GetAnimeRatingSummaryUseCase
import su.afk.yummy.tv.domain.account.GetAnimeUserRatingUseCase
import su.afk.yummy.tv.domain.account.SetAnimeRatingUseCase
import su.afk.yummy.tv.feature.details.presentation.R

@HiltViewModel(assistedFactory = RatingViewModel.Factory::class)
class RatingViewModel @AssistedInject constructor(
    @Assisted private val animeId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val getAnimeRatingSummary: GetAnimeRatingSummaryUseCase,
    private val getAnimeListStats: GetAnimeListStatsUseCase,
    private val getAnimeUserRating: GetAnimeUserRatingUseCase,
    private val setAnimeRating: SetAnimeRatingUseCase,
    private val deleteAnimeRating: DeleteAnimeRatingUseCase,
    private val stringProvider: StringProvider,
) : BaseViewModelNew<RatingState.State, RatingState.Event, RatingState.Effect>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): RatingViewModel
    }

    override fun createInitialState() = RatingState.State()

    init {
        load()
    }

    override fun onEvent(event: RatingState.Event) {
        when (event) {
            RatingState.Event.BackSelected -> nav.back()
            RatingState.Event.RetrySelected -> load()
            is RatingState.Event.RatingSelected -> setRating(event.rating)
            RatingState.Event.RatingDeleted -> deleteRating()
        }
    }

    private fun load() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            val rating = runCatching { getAnimeRatingSummary(animeId) }
            val stats = runCatching { getAnimeListStats(animeId) }
            val userRating = runCatching { getAnimeUserRating(animeId) }

            if (rating.isFailure && stats.isFailure && userRating.isFailure) {
                setState {
                    copy(
                        isLoading = false,
                        error = rating.exceptionOrNull()?.message
                            ?: stats.exceptionOrNull()?.message
                            ?: userRating.exceptionOrNull()?.message
                            ?: stringProvider.get(R.string.details_load_error),
                    )
                }
                return@launch
            }

            setState {
                copy(
                    isLoading = false,
                    error = null,
                    ratingSummary = rating.getOrDefault(ratingSummary),
                    listStats = stats.getOrDefault(listStats),
                    selectedUserRating = userRating.getOrNull(),
                )
            }
        }
    }

    private fun setRating(rating: Int) {
        val previous = currentState.selectedUserRating
        setState { copy(selectedUserRating = rating) }
        viewModelScope.launch {
            val result = runCatching { setAnimeRating(animeId, rating) }
            if (result.isFailure) {
                setState { copy(selectedUserRating = previous) }
            } else {
                refreshRatingSummary()
            }
        }
    }

    private fun deleteRating() {
        val previous = currentState.selectedUserRating
        setState { copy(selectedUserRating = null) }
        viewModelScope.launch {
            val result = runCatching { deleteAnimeRating(animeId) }
            if (result.isFailure) {
                setState { copy(selectedUserRating = previous) }
            } else {
                refreshRatingSummary()
            }
        }
    }

    private suspend fun refreshRatingSummary() {
        runCatching { getAnimeRatingSummary(animeId) }
            .onSuccess { summary -> setState { copy(ratingSummary = summary) } }
    }
}
