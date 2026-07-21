package su.afk.yummy.tv.domain.comments.model

data class CommentsPage(
    val comments: List<Comment>,
    val isModerator: Boolean,
)
