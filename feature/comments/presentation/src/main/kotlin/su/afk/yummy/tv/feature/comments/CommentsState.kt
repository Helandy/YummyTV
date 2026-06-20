package su.afk.yummy.tv.feature.comments

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.comments.model.Comment
import su.afk.yummy.tv.domain.comments.model.CommentReportReason
import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.domain.comments.model.CommentVote

class CommentsState {
    data class State(
        val error: String? = null,
        val comments: Flow<PagingData<CommentUi>> = flowOf(PagingData.empty()),
        val prependedComments: List<CommentUi> = emptyList(),
        val commentOverlays: Map<Int, CommentUi> = emptyMap(),
        val deletedCommentIds: Set<Int> = emptySet(),
        val sort: CommentSort = CommentSort.BEST,
        val isModerator: Boolean = false,
        val currentUserId: Int = 0,
        val composerText: String = "",
        val composerMode: ComposerMode = ComposerMode.New,
        val pendingDelete: Comment? = null,
        val pendingReport: Comment? = null,
        val isMutating: Boolean = false,
    ) : UiState {
        val isSignedIn: Boolean get() = currentUserId > 0
    }

    data class CommentUi(
        val comment: Comment,
        val children: List<CommentUi> = emptyList(),
        val childrenVisible: Boolean = false,
        val childrenLoading: Boolean = false,
        val childrenError: String? = null,
        val childrenHasMore: Boolean = false,
    )

    sealed interface ComposerMode {
        data object New : ComposerMode
        data class Reply(
            val parentCommentId: Int,
            val replyToCommentId: Int,
            val replyToName: String,
            val replyToAvatarUrl: String?,
        ) : ComposerMode

        data class Edit(val commentId: Int) : ComposerMode
    }

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetrySelected : Event
        data object RefreshSelected : Event
        data class SortSelected(val sort: CommentSort) : Event
        data class VisibleCommentsChanged(val comments: List<CommentUi>) : Event
        data class ComposerTextChanged(val text: String) : Event
        data object SubmitSelected : Event
        data object ComposerCancelled : Event
        data class ReplySelected(val commentId: Int) : Event
        data class EditSelected(val commentId: Int) : Event
        data class DeleteSelected(val commentId: Int) : Event
        data object DeleteConfirmed : Event
        data object DeleteDismissed : Event
        data class ReportSelected(val commentId: Int) : Event
        data class ReportConfirmed(val reason: CommentReportReason) : Event
        data object ReportDismissed : Event
        data class VoteSelected(val commentId: Int, val vote: CommentVote) : Event
        data class ChildrenToggleSelected(val commentId: Int) : Event
        data class LoadMoreChildrenSelected(val commentId: Int) : Event
        data class AuthorSelected(val userId: Int) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
    }
}
