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
