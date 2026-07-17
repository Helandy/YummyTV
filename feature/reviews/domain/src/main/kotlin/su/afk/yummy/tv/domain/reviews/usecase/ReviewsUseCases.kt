package su.afk.yummy.tv.domain.reviews.usecase

import su.afk.yummy.tv.domain.reviews.ReviewMutationNotifier
import su.afk.yummy.tv.domain.reviews.model.ReviewSort
import su.afk.yummy.tv.domain.reviews.model.ReviewVote
import su.afk.yummy.tv.domain.reviews.repository.ReviewsRepository
import javax.inject.Inject

class ReviewsUseCases @Inject constructor(
    private val repository: ReviewsRepository,
    private val mutationNotifier: ReviewMutationNotifier,
) {
    suspend fun feedPage(sort: ReviewSort, limit: Int, offset: Int) =
        repository.getReviews(sort, limit, offset)

    suspend fun page(animeId: Int, sort: ReviewSort, limit: Int, offset: Int) =
        repository.getAnimeReviews(animeId, sort, limit, offset)

    suspend fun details(reviewId: Int) = repository.getReview(reviewId)
    suspend fun delete(reviewId: Int) = repository.delete(reviewId).also { deleted ->
        if (deleted) mutationNotifier.notifyChanged()
    }

    suspend fun vote(reviewId: Int, vote: ReviewVote) =
        repository.vote(reviewId, vote).also { mutationNotifier.notifyChanged() }
}
