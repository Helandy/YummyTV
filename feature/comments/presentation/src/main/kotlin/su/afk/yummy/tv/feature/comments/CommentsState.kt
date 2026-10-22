package su.afk.yummy.tv.feature.comments

import androidx.compose.runtime.Immutable
import androidx.paging.PagingData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.comments.model.Comment
import su.afk.yummy.tv.domain.comments.model.CommentReportReason
import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.domain.comments.model.CommentVote

class CommentsState {
    @Immutable
    data class State(
        val error: UiText? = null,
        val comments: Flow<PagingData<CommentUi>> = flowOf(PagingData.empty()),
        val prependedComments: ImmutableList<CommentUi> = persistentListOf(),
        val commentOverlays: PersistentMap<Int, CommentUi> = persistentMapOf(),
        val deletedCommentIds: PersistentSet<Int> = persistentSetOf(),
        val sort: CommentSort = CommentSort.BEST,
        val isModerator: Boolean = false,
        val currentUserId: Int = 0,
        val composerText: String = "",
        val composerMode: ComposerMode = ComposerMode.New,
        val pendingDelete: Comment? = null,
        val pendingReport: Comment? = null,
        val isMutating: Boolean = false,
        /** Число комментариев с ответами; `null`, если экран открыт без известного числа. */
        val totalCount: Int? = null,
    ) : UiState {
        val isSignedIn: Boolean get() = currentUserId > 0
    }

    @Immutable
    data class CommentUi(
        val comment: Comment,
        val children: ImmutableList<CommentUi> = persistentListOf(),
        val childrenVisible: Boolean = false,
        val childrenLoading: Boolean = false,
        val childrenError: UiText? = null,
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
        data class AnimeSelected(val animeId: Int) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
    }
}
