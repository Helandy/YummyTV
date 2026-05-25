package su.afk.yummy.tv.domain.account

data class AnimeRatingSummary(
    val distribution: List<AnimeRatingBucket> = emptyList(),
    val userRating: Int? = null,
)

data class AnimeRatingBucket(
    val rating: Int,
    val count: Int,
)
