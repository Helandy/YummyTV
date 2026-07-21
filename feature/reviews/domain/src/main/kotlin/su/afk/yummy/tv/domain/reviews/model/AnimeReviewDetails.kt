package su.afk.yummy.tv.domain.reviews.model

data class AnimeReviewDetails(
    val review: AnimeReviewSummary,
    val animeTitle: String,
    val animePosterUrl: String?,
    val commentsCount: Int,
)
