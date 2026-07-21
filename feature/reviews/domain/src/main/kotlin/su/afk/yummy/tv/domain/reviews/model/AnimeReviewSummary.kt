package su.afk.yummy.tv.domain.reviews.model

data class AnimeReviewSummary(
    val id: Int,
    val animeId: Int,
    val status: ReviewStatus,
    val author: ReviewAuthor,
    val createdAtSeconds: Long,
    val updatedAtSeconds: Long,
    val views: Int,
    val rating: ReviewRating?,
    val reactions: ReviewReactions,
    val html: String,
    val checkComment: String?,
    val commentable: Boolean,
    val animeTitle: String = "",
    val animePosterUrl: String? = null,
)
