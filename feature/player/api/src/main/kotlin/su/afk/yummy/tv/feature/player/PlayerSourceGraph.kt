package su.afk.yummy.tv.feature.player

import kotlinx.serialization.Serializable

@Serializable
data class PlayerSourceGraph(
    val balancers: List<PlayerSourceBalancer> = emptyList(),
    val selection: PlayerSourceSelection = PlayerSourceSelection(),
)

@Serializable
data class PlayerSourceBalancer(
    val name: String,
    val dubbings: List<PlayerSourceDubbing> = emptyList(),
)

@Serializable
data class PlayerSourceDubbing(
    val name: String,
    val episodes: List<PlayerSourceEpisode> = emptyList(),
    val views: Int = 0,
)

@Serializable
data class PlayerSourceEpisode(
    val id: Int = 0,
    val playerId: Int? = null,
    val number: String = "",
    val iframeUrl: String = "",
    val screenshotUrl: String = "",
    val skips: PlayerSkips = PlayerSkips.Empty,
)

@Serializable
data class PlayerSourceSelection(
    val balancerIndex: Int = 0,
    val dubbingIndex: Int = 0,
    val episodeIndex: Int = 0,
)
