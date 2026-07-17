package su.afk.yummy.tv.feature.reviews.details

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewDetails
import su.afk.yummy.tv.domain.reviews.model.ReviewVote

class ReviewDetailsState {
    data class State(
        val loading: Boolean = true,
        val details: AnimeReviewDetails? = null,
        val currentUserId: Int = 0,
        val error: String? = null,
        val deleting: Boolean = false,
    ) : UiState {
        val isOwner get() = currentUserId > 0 && details?.review?.author?.id == currentUserId
    }

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetrySelected : Event
        data object DeleteConfirmed : Event
        data class VoteSelected(val vote: ReviewVote) : Event
        data class AuthorSelected(val userId: Int) : Event
        data class AnimeSelected(val animeId: Int) : Event
        data object CommentsSelected : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data object Deleted : Effect
    }
}
