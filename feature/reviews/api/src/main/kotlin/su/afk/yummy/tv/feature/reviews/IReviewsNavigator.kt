package su.afk.yummy.tv.feature.reviews

import androidx.navigation3.runtime.NavKey

interface IReviewsNavigator {
    fun feed(): NavKey
    fun list(animeId: Int): NavKey
    fun details(reviewId: Int): NavKey
}
