package su.afk.yummy.tv.domain.posts.model

data class PostDetails(
    val id: Int,
    val title: String,
    val contentHtml: String,
    val previewImageUrl: String?,
    val author: PostAuthor,
    val category: PostCategory,
    val createdAt: Long,
    val editedAt: Long?,
    val relatedAnime: List<RelatedPostAnime>,
    val reaction: PostReaction,
    val views: Int,
    val comments: Int,
)
