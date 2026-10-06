package su.afk.yummy.tv.feature.reviews

import su.afk.yummy.tv.domain.reviews.model.AnimeReviewDetails
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewSummary
import su.afk.yummy.tv.domain.reviews.model.ReviewAuthor
import su.afk.yummy.tv.domain.reviews.model.ReviewReactions
import su.afk.yummy.tv.domain.reviews.model.ReviewStatus
import su.afk.yummy.tv.domain.reviews.model.ReviewVote

internal fun reviewSummary(
    id: Int = 1,
    authorId: Int = 10,
    reactions: ReviewReactions = ReviewReactions(likes = 1, dislikes = 0, vote = ReviewVote.NONE),
) = AnimeReviewSummary(
    id = id,
    animeId = 5,
    status = ReviewStatus.APPROVED,
    author = ReviewAuthor(id = authorId, nickname = "author", avatarUrl = null),
    createdAtSeconds = 0L,
    updatedAtSeconds = 0L,
    views = 0,
    rating = null,
    reactions = reactions,
    html = "",
    checkComment = null,
    commentable = true,
)

internal fun reviewDetails(
    reviewId: Int = 1,
    authorId: Int = 10,
    reactions: ReviewReactions = ReviewReactions(likes = 1, dislikes = 0, vote = ReviewVote.NONE),
) = AnimeReviewDetails(
    review = reviewSummary(id = reviewId, authorId = authorId, reactions = reactions),
    animeTitle = "Anime",
    animePosterUrl = null,
    commentsCount = 0,
)
