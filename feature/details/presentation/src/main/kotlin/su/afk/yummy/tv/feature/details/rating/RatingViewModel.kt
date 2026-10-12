package su.afk.yummy.tv.feature.details.rating

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.rating.handler.RatingMutationHandler
import su.afk.yummy.tv.feature.details.rating.handler.RatingMutationResult

@HiltViewModel(assistedFactory = RatingViewModel.Factory::class)
class RatingViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val ratingMutationHandler: RatingMutationHandler,
    private val settingsStore: YaniAccountSettingsStore,
    private val stringProvider: StringProvider,
    private val analytics: DetailsAnalytics,
) : BaseViewModel<RatingState.State, RatingState.Event, RatingState.Effect>() {

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
            val result = ratingMutationHandler.load(animeId)

            if (result.ratingSummary.isFailure && result.userRating.isFailure) {
                val error = result.ratingSummary.exceptionOrNull()
                    ?: result.userRating.exceptionOrNull()
                error?.let(analytics::eventRatingLoadError)
                setState {
                    copy(
                        isLoading = false,
                        error = UiText.Failure(
                            cause = error ?: IllegalStateException(),
                            fallback = UiText.Res(R.string.details_load_error),
                        ),
                    )
                }
                return@launch
            }

            setState {
                copy(
                    isLoading = false,
                    error = null,
                    ratingSummary = result.ratingSummary.getOrDefault(ratingSummary),
                    listStats = result.listStats.getOrDefault(listStats),
                    selectedUserRating = result.userRating.getOrNull(),
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
            when (ratingMutationHandler.setRating(animeId, rating)) {
                RatingMutationResult.Success -> refreshRatingSummary()
                RatingMutationResult.Failure -> setState { copy(selectedUserRating = previous) }
            }
        }
    }

    private fun deleteRating() {
        viewModelScope.launch {
            if (!canMutateRating()) return@launch
            analytics.eventRatingDeleted(animeId)
            val previous = currentState.selectedUserRating
            setState { copy(selectedUserRating = null) }
            when (ratingMutationHandler.deleteRating(animeId)) {
                RatingMutationResult.Success -> refreshRatingSummary()
                RatingMutationResult.Failure -> setState { copy(selectedUserRating = previous) }
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
        ratingMutationHandler.refreshSummary(animeId)?.let { summary ->
            setState { copy(ratingSummary = summary) }
        }
    }

}
