package su.afk.yummy.tv.feature.details.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.analytics.AnalyticsDestination

@Serializable
data class DetailsDestination(val animeId: Int) : NavKey, AnalyticsDestination {
    override val screenName: String = "details"
    override val screenParams: Map<String, String>
        get() = animeParams(animeId)
}

@Serializable
data class DetailsFullDestination(val animeId: Int) : NavKey, AnalyticsDestination {
    override val screenName: String = "details_full"
    override val screenParams: Map<String, String>
        get() = animeParams(animeId)
}

@Serializable
data class DetailsEpisodesDestination(val animeId: Int) : NavKey, AnalyticsDestination {
    override val screenName: String = "details_episodes"
    override val screenParams: Map<String, String>
        get() = animeParams(animeId)
}

@Serializable
data class DetailsEpisodeDubbingsDestination(
    val animeId: Int,
    val episode: String,
) : NavKey, AnalyticsDestination {
    override val screenName: String = "details_episode_dubbings"
    override val screenParams: Map<String, String>
        get() = animeParams(animeId)
}

@Serializable
data class DetailsSubscriptionsDestination(val animeId: Int) : NavKey, AnalyticsDestination {
    override val screenName: String = "details_subscriptions"
    override val screenParams: Map<String, String>
        get() = animeParams(animeId)
}

@Serializable
data class DetailsTrailersDestination(val animeId: Int) : NavKey, AnalyticsDestination {
    override val screenName: String = "details_trailers"
    override val screenParams: Map<String, String>
        get() = animeParams(animeId)
}

@Serializable
data class DetailsSimilarDestination(val animeId: Int) : NavKey, AnalyticsDestination {
    override val screenName: String = "details_similar"
    override val screenParams: Map<String, String>
        get() = animeParams(animeId)
}

@Serializable
data class DetailsViewingOrderDestination(val animeId: Int) : NavKey, AnalyticsDestination {
    override val screenName: String = "details_viewing_order"
    override val screenParams: Map<String, String>
        get() = animeParams(animeId)
}

@Serializable
data class DetailsScreenshotsDestination(val animeId: Int) : NavKey, AnalyticsDestination {
    override val screenName: String = "details_screenshots"
    override val screenParams: Map<String, String>
        get() = animeParams(animeId)
}

@Serializable
data class DetailsRatingDestination(val animeId: Int) : NavKey, AnalyticsDestination {
    override val screenName: String = "details_rating"
    override val screenParams: Map<String, String>
        get() = animeParams(animeId)
}

@Serializable
data class DetailsCollectionsDestination(val animeId: Int) : NavKey, AnalyticsDestination {
    override val screenName: String = "details_collections"
    override val screenParams: Map<String, String>
        get() = animeParams(animeId)
}

private fun animeParams(animeId: Int): Map<String, String> =
    mapOf("anime_id" to animeId.toString())
