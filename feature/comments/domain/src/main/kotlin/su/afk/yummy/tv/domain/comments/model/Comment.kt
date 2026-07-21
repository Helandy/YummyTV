package su.afk.yummy.tv.domain.comments.model

data class Comment(
    val id: Int,
    val author: CommentAuthor,
    val text: String,
    val createdAtEpochSeconds: Long,
    val parentId: Int?,
    val childrenCount: Int,
    val likes: Int,
    val dislikes: Int,
    val vote: CommentVote,
    val roles: List<String>,
    val deletedAtEpochSeconds: Long?,
)
