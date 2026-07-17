package su.afk.yummy.tv.feature.posts.details

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.posts.model.PostDetails
import su.afk.yummy.tv.domain.posts.model.PostVote

object PostDetailsState {
    data class State(
        val loading: Boolean = true,
        val details: PostDetails? = null,
        val currentUserId: Int = 0,
        val error: String? = null,
        val voting: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetrySelected : Event
        data class VoteSelected(val vote: PostVote) : Event
        data class AnimeSelected(val animeId: Int) : Event
        data class AuthorSelected(val userId: Int) : Event
        data object CommentsSelected : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
    }
}
