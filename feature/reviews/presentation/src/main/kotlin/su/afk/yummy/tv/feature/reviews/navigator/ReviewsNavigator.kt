package su.afk.yummy.tv.feature.reviews.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.reviews.IReviewsNavigator
import javax.inject.Inject

class ReviewsNavigator @Inject constructor() : IReviewsNavigator {
    override fun feed(): NavKey = ReviewsDestination
    override fun list(animeId: Int): NavKey = AnimeReviewsDestination(animeId)
    override fun details(reviewId: Int): NavKey = ReviewDetailsDestination(reviewId)
}
