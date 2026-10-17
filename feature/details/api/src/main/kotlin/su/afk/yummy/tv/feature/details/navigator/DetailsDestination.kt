package su.afk.yummy.tv.feature.details.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.navigation.scene.FullscreenDestination

@Serializable
data class DetailsDestination(val animeId: Int) : NavKey

@Serializable
data class DetailsFullDestination(val animeId: Int) : NavKey

@Serializable
data class DetailsEpisodesDestination(val animeId: Int, val pendingEpisode: String? = null) : NavKey

@Serializable
data class DetailsEpisodeDubbingsDestination(
    val animeId: Int,
    val episode: String,
) : NavKey

@Serializable
data class DetailsSubscriptionsDestination(val animeId: Int) : NavKey

@Serializable
data class DetailsTrailersDestination(val animeId: Int) : NavKey

/** Встроенный просмотр трейлера. [videoId] задан для YouTube, иначе играется [embedUrl]. */
@Serializable
data class DetailsTrailerPlayerDestination(
    val videoId: String?,
    val embedUrl: String,
    val externalUrl: String,
) : NavKey, FullscreenDestination

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
