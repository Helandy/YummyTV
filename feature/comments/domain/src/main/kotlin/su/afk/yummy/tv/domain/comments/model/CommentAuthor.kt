package su.afk.yummy.tv.domain.comments.model

data class CommentAuthor(
    val id: Int,
    val name: String,
    val avatarSmallUrl: String?,
    val avatarBigUrl: String?,
    val avatarFullUrl: String?,
)
