package su.afk.yummy.tv.feature.comments.utils

import su.afk.yummy.tv.domain.comments.model.Comment
import su.afk.yummy.tv.domain.comments.model.CommentVote
import su.afk.yummy.tv.domain.comments.model.CommentVoteResult
import su.afk.yummy.tv.feature.comments.CommentsState.CommentUi

internal fun List<CommentUi>.findUi(commentId: Int): CommentUi? {
    forEach { item ->
        if (item.comment.id == commentId) return item
        item.children.findUi(commentId)?.let { return it }
    }
    return null
}

internal fun List<CommentUi>.updateCommentUi(
    commentId: Int,
    transform: CommentUi.() -> CommentUi,
): List<CommentUi> = map { item ->
    when {
        item.comment.id == commentId -> item.transform()
        item.children.isNotEmpty() -> item.copy(
            children = item.children.updateCommentUi(
                commentId,
                transform
            )
        )

        else -> item
    }
}

internal fun List<CommentUi>.replaceComment(comment: Comment): List<CommentUi> =
    updateCommentUi(comment.id) { copy(comment = comment) }

internal fun List<CommentUi>.removeComment(commentId: Int): List<CommentUi> =
    mapNotNull { item ->
        when {
            item.comment.id == commentId -> null
            item.children.isNotEmpty() -> {
                val newChildren = item.children.removeComment(commentId)
                item.copy(
                    children = newChildren,
                    comment = item.comment.copy(
                        childrenCount = (item.comment.childrenCount - (item.children.size - newChildren.size))
                            .coerceAtLeast(0),
                    ),
                )
            }

            else -> item
        }
    }

internal fun List<CommentUi>.updateVote(
    commentId: Int,
    result: CommentVoteResult,
    vote: CommentVote,
): List<CommentUi> =
    updateCommentUi(commentId) {
        copy(
            comment = comment.copy(
                likes = result.likes,
                dislikes = result.dislikes,
                vote = vote,
            ),
        )
    }
