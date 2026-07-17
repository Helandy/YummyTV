package su.afk.yummy.tv.domain.reviews.model

enum class ReviewStatus { APPROVED, WAITING, DECLINED }
enum class ReviewSort(val apiValue: String) { NEW("new"), OLD("old"), TOP("top") }
enum class ReviewVote(val apiValue: Int) { LIKE(1), NONE(0), DISLIKE(-1) }

data class ReviewRatingCategory(val name: String, val score: Int)
data class ReviewAuthor(val id: Int, val nickname: String, val avatarUrl: String?)
data class ReviewRating(val average: Int?, val categories: List<ReviewRatingCategory>)
data class ReviewReactions(val likes: Int, val dislikes: Int, val vote: ReviewVote)

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

data class AnimeReviewDetails(
    val review: AnimeReviewSummary,
    val animeTitle: String,
    val animePosterUrl: String?,
    val commentsCount: Int,
)

data class ReviewPage(val reviews: List<AnimeReviewSummary>)
