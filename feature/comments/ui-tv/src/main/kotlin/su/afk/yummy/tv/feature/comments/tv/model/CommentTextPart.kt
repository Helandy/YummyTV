package su.afk.yummy.tv.feature.comments.tv.model

internal sealed interface CommentTextPart {
    data class Plain(val text: String) : CommentTextPart
    data class Spoiler(val title: String, val text: String) : CommentTextPart
}
