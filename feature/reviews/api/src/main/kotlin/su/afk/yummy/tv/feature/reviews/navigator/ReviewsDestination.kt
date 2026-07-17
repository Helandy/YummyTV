package su.afk.yummy.tv.feature.reviews.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object ReviewsDestination : NavKey

@Serializable
data class AnimeReviewsDestination(val animeId: Int) : NavKey

@Serializable
data class ReviewDetailsDestination(val reviewId: Int) : NavKey
