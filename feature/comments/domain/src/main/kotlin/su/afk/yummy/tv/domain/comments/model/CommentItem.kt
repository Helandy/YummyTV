package su.afk.yummy.tv.domain.comments.model

/** Тайтл, к которому относится комментарий (известен в ленте комментариев пользователя). */
data class CommentItem(
    val animeId: Int,
    val title: String,
    val posterUrl: String?,
)
