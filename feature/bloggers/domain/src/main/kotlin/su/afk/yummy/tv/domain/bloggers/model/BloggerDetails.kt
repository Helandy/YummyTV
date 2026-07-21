package su.afk.yummy.tv.domain.bloggers.model

data class BloggerDetails(
    val id: Int,
    val nickname: String,
    val avatarUrl: String?,
    val subscribers: Int,
    val videosCount: Int,
    val isSubscribed: Boolean,
    val categories: List<BloggerVideoCategory>,
)
