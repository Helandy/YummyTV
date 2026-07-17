package su.afk.yummy.tv.domain.bloggers.model

data class BloggerVideo(
    val id: Int,
    val title: String,
    val description: String,
    val previewUrl: String?,
    val iframeUrl: String,
    val publishedAt: Long,
    val views: Long,
    val hasSpoiler: Boolean,
    val category: BloggerVideoCategory,
    val creator: Blogger,
    val reaction: BloggerVideoReaction = BloggerVideoReaction(),
    val commentsCount: Int = 0,
) {
    val watchUrl: String
        get() {
            val videoId = iframeUrl.substringAfter("/embed/", "")
                .substringBefore('?')
                .takeIf(String::isNotBlank)
            return videoId?.let { "https://www.youtube.com/watch?v=$it" } ?: iframeUrl
        }
}

data class BloggerVideoReaction(
    val likes: Int = 0,
    val dislikes: Int = 0,
    val vote: BloggerVideoVote = BloggerVideoVote.NONE,
)

enum class BloggerVideoVote(val apiValue: String?) {
    NONE(null), LIKE("like"), DISLIKE("dislike")
}

data class BloggerVideoCategory(val id: String, val title: String)

data class Blogger(
    val id: Int,
    val nickname: String,
    val avatarUrl: String?,
)

data class BloggerDetails(
    val id: Int,
    val nickname: String,
    val avatarUrl: String?,
    val subscribers: Int,
    val videosCount: Int,
    val isSubscribed: Boolean,
    val categories: List<BloggerVideoCategory>,
)

data class BloggerDirectory(
    val categories: List<BloggerVideoCategory>,
    val bloggers: List<Blogger>,
)

enum class BloggerVideoSort(val apiValue: String) {
    NEW("new"), TOP("top"), OLD("old")
}
