package su.afk.yummy.tv.domain.comments.repository

import su.afk.yummy.tv.domain.comments.model.Comment
import su.afk.yummy.tv.domain.comments.model.CommentDraft
import su.afk.yummy.tv.domain.comments.model.CommentReportReason
import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.domain.comments.model.CommentTargetType
import su.afk.yummy.tv.domain.comments.model.CommentVote
import su.afk.yummy.tv.domain.comments.model.CommentVoteResult
import su.afk.yummy.tv.domain.comments.model.CommentsPage

interface CommentsRepository {
    suspend fun getComments(
        targetType: CommentTargetType,
        targetId: Int,
        limit: Int,
        skip: Int,
        sort: CommentSort,
        forceRefresh: Boolean = false,
    ): CommentsPage

    suspend fun getCommentChildren(
        commentId: Int,
        skip: Int,
    ): CommentsPage

    suspend fun addComment(
        targetType: CommentTargetType,
        targetId: Int,
        draft: CommentDraft,
    ): Comment

    suspend fun updateComment(
        commentId: Int,
        text: String,
    ): Comment

    suspend fun deleteComment(commentId: Int): Boolean

    suspend fun voteComment(
        commentId: Int,
        vote: CommentVote,
    ): CommentVoteResult

    suspend fun removeCommentVote(commentId: Int): CommentVoteResult

    suspend fun reportComment(
        commentId: Int,
        reason: CommentReportReason,
    ): Boolean
}
