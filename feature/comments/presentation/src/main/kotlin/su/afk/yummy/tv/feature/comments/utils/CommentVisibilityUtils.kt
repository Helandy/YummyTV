package su.afk.yummy.tv.feature.comments.utils

import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.feature.comments.CommentsState

/** Применяет к комментарию оверлеи и удаления из состояния; удалённый комментарий превращается в null. */
fun CommentsState.CommentUi.resolve(
    state: CommentsState.State,
): CommentsState.CommentUi? {
    if (comment.id in state.deletedCommentIds) return null
    val overlaid = state.commentOverlays[comment.id] ?: this
    return overlaid.copy(
        children = overlaid.children.mapNotNull { it.resolve(state) }.toImmutableList(),
    )
}

/** Объединяет локально добавленные комментарии с загруженной страницей и применяет оверлеи. */
fun buildVisibleComments(
    state: CommentsState.State,
    pagedComments: List<CommentsState.CommentUi>,
): List<CommentsState.CommentUi> =
    (state.prependedComments + pagedComments)
        .distinctBy { it.comment.id }
        .mapNotNull { it.resolve(state) }
