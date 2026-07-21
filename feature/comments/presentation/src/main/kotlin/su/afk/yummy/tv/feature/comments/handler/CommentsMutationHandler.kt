package su.afk.yummy.tv.feature.comments.handler

import su.afk.yummy.tv.domain.comments.model.Comment
import su.afk.yummy.tv.domain.comments.model.CommentDraft
import su.afk.yummy.tv.domain.comments.model.CommentReportReason
import su.afk.yummy.tv.domain.comments.model.CommentTargetType
import su.afk.yummy.tv.domain.comments.model.CommentVote
import su.afk.yummy.tv.domain.comments.model.CommentVoteResult
import su.afk.yummy.tv.domain.comments.usecase.AddCommentUseCase
import su.afk.yummy.tv.domain.comments.usecase.DeleteCommentUseCase
import su.afk.yummy.tv.domain.comments.usecase.RemoveCommentVoteUseCase
import su.afk.yummy.tv.domain.comments.usecase.ReportCommentUseCase
import su.afk.yummy.tv.domain.comments.usecase.UpdateCommentUseCase
import su.afk.yummy.tv.domain.comments.usecase.VoteCommentUseCase
import javax.inject.Inject

class CommentsMutationHandler @Inject constructor(
    private val addComment: AddCommentUseCase,
    private val updateComment: UpdateCommentUseCase,
    private val deleteComment: DeleteCommentUseCase,
    private val voteComment: VoteCommentUseCase,
    private val removeCommentVote: RemoveCommentVoteUseCase,
    private val reportComment: ReportCommentUseCase,
) {

    suspend fun create(
        targetType: CommentTargetType,
        targetId: Int,
        draft: CommentDraft,
    ): Result<Comment> = runCatching {
        addComment(targetType, targetId, draft)
    }

    suspend fun update(commentId: Int, text: String): Result<Comment> = runCatching {
        updateComment(commentId, text)
    }

    suspend fun delete(commentId: Int): Result<Boolean> = runCatching {
        deleteComment(commentId)
    }

    suspend fun report(
        commentId: Int,
        reason: CommentReportReason,
    ): Result<Boolean> = runCatching {
        reportComment(commentId, reason)
    }

    suspend fun changeVote(
        commentId: Int,
        currentVote: CommentVote,
        selectedVote: CommentVote,
    ): Result<CommentVoteChange> = runCatching {
        if (currentVote == selectedVote) {
            CommentVoteChange(
                result = removeCommentVote(commentId),
                vote = CommentVote.NEUTRAL,
            )
        } else {
            CommentVoteChange(
                result = voteComment(commentId, selectedVote),
                vote = selectedVote,
            )
        }
    }
}

data class CommentVoteChange(
    val result: CommentVoteResult,
    val vote: CommentVote,
)
