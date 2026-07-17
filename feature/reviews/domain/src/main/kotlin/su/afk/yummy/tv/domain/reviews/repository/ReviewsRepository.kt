package su.afk.yummy.tv.domain.reviews.repository

import su.afk.yummy.tv.domain.reviews.model.AnimeReviewDetails
import su.afk.yummy.tv.domain.reviews.model.ReviewPage
import su.afk.yummy.tv.domain.reviews.model.ReviewReactions
import su.afk.yummy.tv.domain.reviews.model.ReviewSort
import su.afk.yummy.tv.domain.reviews.model.ReviewVote

interface ReviewsRepository {
    suspend fun getReviews(sort: ReviewSort, limit: Int, offset: Int): ReviewPage
    suspend fun getAnimeReviews(animeId: Int, sort: ReviewSort, limit: Int, offset: Int): ReviewPage
    suspend fun getReview(reviewId: Int): AnimeReviewDetails
    suspend fun delete(reviewId: Int): Boolean
    suspend fun vote(reviewId: Int, vote: ReviewVote): ReviewReactions
}
