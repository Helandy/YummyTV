package su.afk.yummy.tv.data.reviews.mapper

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import su.afk.yummy.tv.core.utils.network.toHttpsUrlOrNull
import su.afk.yummy.tv.data.reviews.dto.YaniReviewDto
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewDetails
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewSummary
import su.afk.yummy.tv.domain.reviews.model.ReviewAuthor
import su.afk.yummy.tv.domain.reviews.model.ReviewPage
import su.afk.yummy.tv.domain.reviews.model.ReviewRating
import su.afk.yummy.tv.domain.reviews.model.ReviewRatingCategory
import su.afk.yummy.tv.domain.reviews.model.ReviewReactions
import su.afk.yummy.tv.domain.reviews.model.ReviewStatus
import su.afk.yummy.tv.domain.reviews.model.ReviewVote

internal fun YaniReviewDto.toSummaryOrNull(): AnimeReviewSummary? {
    if (reviewId <= 0) return null
    return AnimeReviewSummary(
        id = reviewId,
        animeId = animeId,
        status = when (type) {
            "waiting" -> ReviewStatus.WAITING; "declined" -> ReviewStatus.DECLINED; else -> ReviewStatus.APPROVED
        },
        author = ReviewAuthor(
            author.id ?: userId ?: 0,
            author.nickname ?: nickname.orEmpty(),
            author.avatars?.run { full ?: big ?: small }.toHttpsUrlOrNull()
        ),
        createdAtSeconds = createDate,
        updatedAtSeconds = updateDate,
        views = views,
        rating = rating?.let {
            ReviewRating(
                (it.average as? JsonPrimitive)?.doubleOrNull?.toInt(),
                it.category.orEmpty().map { (name, score) -> ReviewRatingCategory(name, score) })
        },
        reactions = ReviewReactions(
            likes.likes,
            likes.dislikes,
            ReviewVote.entries.firstOrNull { it.apiValue == likes.vote } ?: ReviewVote.NONE),
        html = textHtml.ifBlank { textPreview },
        checkComment = checkComment,
        commentable = commentable,
        animeTitle = anime?.title.orEmpty(),
        animePosterUrl = anime?.poster?.run { mega ?: huge ?: big ?: medium ?: small ?: fullsize }
            .toHttpsUrlOrNull(),
    )
}

internal fun List<YaniReviewDto>.toReviewPage(): ReviewPage =
    ReviewPage(mapNotNull { it.toSummaryOrNull() })

internal fun YaniReviewDto.toDetailsOrNull(): AnimeReviewDetails? {
    val summary = toSummaryOrNull() ?: return null
    return AnimeReviewDetails(
        review = summary,
        animeTitle = anime?.title.orEmpty(),
        animePosterUrl = anime?.poster
            ?.run { mega ?: huge ?: big ?: medium ?: small ?: fullsize }
            .toHttpsUrlOrNull(),
        commentsCount = commentsCount,
    )
}
