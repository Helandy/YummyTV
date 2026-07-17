package su.afk.yummy.tv.feature.details.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class DetailsDestination(val animeId: Int) : NavKey

@Serializable
data class DetailsFullDestination(val animeId: Int) : NavKey

@Serializable
data class DetailsEpisodesDestination(val animeId: Int) : NavKey

@Serializable
data class DetailsEpisodeDubbingsDestination(
    val animeId: Int,
    val episode: String,
) : NavKey

@Serializable
data class DetailsSubscriptionsDestination(val animeId: Int) : NavKey

@Serializable
data class DetailsTrailersDestination(val animeId: Int) : NavKey

@Serializable
data class DetailsSimilarDestination(val animeId: Int) : NavKey

@Serializable
data class DetailsViewingOrderDestination(val animeId: Int) : NavKey

@Serializable
data class DetailsScreenshotsDestination(val animeId: Int) : NavKey

@Serializable
data class DetailsRatingDestination(val animeId: Int) : NavKey

@Serializable
data class DetailsCollectionsDestination(val animeId: Int) : NavKey

@Serializable
enum class DetailsRelationKind { STUDIO, DIRECTOR, GENRE }

@Serializable
data class DetailsRelationDestination(
    val kind: DetailsRelationKind,
    val id: Int,
    val url: String? = null,
) : NavKey
