package su.afk.yummy.tv.feature.reviews.list

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewSummary
import su.afk.yummy.tv.domain.reviews.model.ReviewReactions
import su.afk.yummy.tv.domain.reviews.model.ReviewSort
import su.afk.yummy.tv.domain.reviews.model.ReviewVote

class ReviewsListState {
    data class State(
        val reviews: Flow<PagingData<AnimeReviewSummary>> = flowOf(PagingData.empty()),
        val sort: ReviewSort = ReviewSort.NEW,
        val currentUserId: Int = 0,
        val reactionOverrides: Map<Int, ReviewReactions> = emptyMap(),
        val isGeneralFeed: Boolean = false,
    ) : UiState {
        val isSignedIn get() = currentUserId > 0
        val availableSorts: List<ReviewSort>
            get() = if (isGeneralFeed) listOf(
                ReviewSort.NEW,
                ReviewSort.TOP
            ) else ReviewSort.entries
    }

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data class ReviewSelected(val id: Int) : Event
        data class AuthorSelected(val userId: Int) : Event
        data class SortSelected(val sort: ReviewSort) : Event
        data class VoteSelected(val review: AnimeReviewSummary, val vote: ReviewVote) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
    }
}
