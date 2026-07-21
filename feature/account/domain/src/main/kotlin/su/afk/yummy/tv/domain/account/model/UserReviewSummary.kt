package su.afk.yummy.tv.domain.account.model

data class UserReviewSummary(
    val id: Int,
    val animeId: Int,
    val animeTitle: String,
    val animePosterUrl: String?,
    val textPreview: String,
    val rating: Double?,
    val likes: Int,
    val dislikes: Int,
    val commentsCount: Int,
    val updatedAtSeconds: Long,
)
