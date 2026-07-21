package su.afk.yummy.tv.domain.comments.model

data class CommentDraft(
    val text: String,
    val parentCommentId: Int? = null,
    val replyToCommentId: Int? = null,
)
