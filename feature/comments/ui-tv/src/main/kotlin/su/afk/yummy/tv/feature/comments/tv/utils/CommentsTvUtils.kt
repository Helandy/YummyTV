package su.afk.yummy.tv.feature.comments.tv.utils

import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.feature.comments.CommentsState
import su.afk.yummy.tv.feature.comments.tv.model.CommentTextPart

internal fun CommentsState.CommentUi.resolve(
    state: CommentsState.State,
): CommentsState.CommentUi? {
    if (comment.id in state.deletedCommentIds) return null
    val overlaid = state.commentOverlays[comment.id] ?: this
    return overlaid.copy(
        children = overlaid.children.mapNotNull { it.resolve(state) }.toImmutableList(),
    )
}

/** Объединяет локально добавленные комментарии с загруженной страницей и применяет оверлеи. */
internal fun buildVisibleComments(
    state: CommentsState.State,
    pagedComments: List<CommentsState.CommentUi>,
): List<CommentsState.CommentUi> =
    (state.prependedComments + pagedComments)
        .distinctBy { it.comment.id }
        .mapNotNull { it.resolve(state) }

private val spoilerRegex = Regex(
    pattern = "\\[спойлер(?:=\"([^\"]*)\")?](.*?)\\[/спойлер]",
    options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val bbCodeRegex = Regex("\\[/?[^\\]]+]")

internal fun parseCommentText(text: String, defaultSpoilerTitle: String): List<CommentTextPart> {
    val result = mutableListOf<CommentTextPart>()
    var cursor = 0
    spoilerRegex.findAll(text).forEach { match ->
        val before = text.substring(cursor, match.range.first)
        if (before.isNotBlank()) result += CommentTextPart.Plain(before.cleanCommentText())
        result += CommentTextPart.Spoiler(
            title = match.groups[1]?.value?.takeIf { it.isNotBlank() } ?: defaultSpoilerTitle,
            text = match.groups[2]?.value.orEmpty().cleanCommentText(),
        )
        cursor = match.range.last + 1
    }
    val tail = text.substring(cursor)
    if (tail.isNotBlank()) result += CommentTextPart.Plain(tail.cleanCommentText())
    return result.ifEmpty { listOf(CommentTextPart.Plain(text.cleanCommentText())) }
}

private fun String.cleanCommentText(): String =
    replace(bbCodeRegex, "")
        .replace("&nbsp;", " ")
        .trim()
