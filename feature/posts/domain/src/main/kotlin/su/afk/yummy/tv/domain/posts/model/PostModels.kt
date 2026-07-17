package su.afk.yummy.tv.domain.posts.model

data class PostCategory(val id: Int, val title: String, val uri: String)

data class PostAuthor(val id: Int, val nickname: String, val avatarUrl: String?)

data class PostSummary(
    val id: Int,
    val title: String,
    val previewImageUrl: String?,
    val contentPreview: String,
    val author: PostAuthor,
    val category: PostCategory,
    val createdAt: Long,
)

data class PostReaction(val likes: Int, val dislikes: Int, val vote: PostVote)

enum class PostVote(val action: Int) { LIKE(1), DISLIKE(-1), NONE(0) }

enum class PostSort(val apiValue: String) { NEW("new"), OLD("old"), BEST("best") }

data class RelatedPostAnime(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val year: Int?,
    val rating: Double?,
)

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
