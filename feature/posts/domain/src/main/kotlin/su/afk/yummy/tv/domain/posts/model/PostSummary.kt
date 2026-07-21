package su.afk.yummy.tv.domain.posts.model

data class PostSummary(
    val id: Int,
    val title: String,
    val previewImageUrl: String?,
    val contentPreview: String,
    val author: PostAuthor,
    val category: PostCategory,
    val createdAt: Long,
)
