package su.afk.yummy.tv.feature.bloggers.video

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoVote

object BloggerVideoDetailsState {
    data class State(
        val video: BloggerVideo? = null,
        val currentUserId: Int = 0,
        val loading: Boolean = true,
        val voting: Boolean = false,
        val error: String? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetrySelected : Event
        data object WatchSelected : Event
        data object BloggerSelected : Event
        data class VoteSelected(val vote: BloggerVideoVote) : Event
        data object CommentsSelected : Event
    }

    sealed interface Effect : UiEffect {
        data class OpenVideo(val url: String) : Effect
        data class ShowToast(val message: String) : Effect
    }
}
