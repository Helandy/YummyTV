package su.afk.yummy.tv.domain.comments.model

data class CommentVoteResult(
    val likes: Int,
    val dislikes: Int,
    val success: Boolean,
)
