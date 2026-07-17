package su.afk.yummy.tv.feature.details.rating

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.utils.runSuspendCatching
import su.afk.yummy.tv.domain.account.usecase.DeleteAnimeRatingUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeListStatsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeRatingSummaryUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeUserRatingUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeRatingUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.presentation.R

@HiltViewModel(assistedFactory = RatingViewModel.Factory::class)
class RatingViewModel @AssistedInject internal constructor(
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
    private val settingsStore: SettingsStore,
    private val stringProvider: StringProvider,
    private val analytics: DetailsAnalytics,
) : BaseViewModelNew<RatingState.State, RatingState.Event, RatingState.Effect>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): RatingViewModel
    }

    override fun createInitialState() = RatingState.State()

    init {
        analytics.eventRatingScreenOpened(animeId)
        load()
    }

    override fun onEvent(event: RatingState.Event) {
        when (event) {
            RatingState.Event.BackSelected -> nav.back()
            RatingState.Event.RetrySelected -> {
                analytics.eventRatingRetry(animeId)
                load()
            }

            is RatingState.Event.RatingSelected -> setRating(event.rating)
            RatingState.Event.RatingDeleted -> deleteRating()
        }
    }

    private fun load() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            val (rating, stats, userRating) = coroutineScope {
                val rating = async { runSuspendCatching { getAnimeRatingSummary(animeId) } }
                val stats = async { runSuspendCatching { getAnimeListStats(animeId) } }
                val userRating = async { runSuspendCatching { getAnimeUserRating(animeId) } }
                Triple(rating.await(), stats.await(), userRating.await())
            }

            if (rating.isFailure && userRating.isFailure) {
                val error = rating.exceptionOrNull()
                    ?: userRating.exceptionOrNull()
                error?.let(analytics::eventRatingLoadError)
                setState {
                    copy(
                        isLoading = false,
                        error = error?.message
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
        viewModelScope.launch {
            if (!canMutateRating()) return@launch
            analytics.eventRatingSelected(animeId, rating)
            val previous = currentState.selectedUserRating
            setState { copy(selectedUserRating = rating) }
            val result = runCatching { setAnimeRating(animeId, rating) }
            if (result.isFailure) {
                setState { copy(selectedUserRating = previous) }
            } else {
                refreshRatingSummary()
            }
        }
    }

    private fun deleteRating() {
        viewModelScope.launch {
            if (!canMutateRating()) return@launch
            analytics.eventRatingDeleted(animeId)
            val previous = currentState.selectedUserRating
            setState { copy(selectedUserRating = null) }
            val result = runCatching { deleteAnimeRating(animeId) }
            if (result.isFailure) {
                setState { copy(selectedUserRating = previous) }
            } else {
                refreshRatingSummary()
            }
        }
    }

    private suspend fun canMutateRating(): Boolean {
        if (settingsStore.yaniUserId.first() > 0) return true
        setEffect(
            RatingState.Effect.ShowToast(
                stringProvider.get(R.string.details_rating_auth_required)
            )
        )
        return false
    }

    private suspend fun refreshRatingSummary() {
        runCatching { getAnimeRatingSummary(animeId) }
            .onSuccess { summary -> setState { copy(ratingSummary = summary) } }
    }

}
