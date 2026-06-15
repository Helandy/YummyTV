package su.afk.yummy.tv.domain.player.model

data class PlayerSourceRequest(
    val animeId: Int,
    val iframeUrl: String,
    val animeTitle: String,
    val episode: String,
    val playerName: String,
    val dubbing: String,
    val selectedVideoId: Int,
    val selectedScreenshotUrl: String,
)

data class PlayerSourceData(
    val videos: List<PlayerSourceVideo>,
    val screenshotByEpisode: Map<String, String> = emptyMap(),
)

data class PlayerSourceVideo(
    val id: Int,
    val episode: String,
    val dubbing: String,
    val player: String,
    val iframeUrl: String,
    val views: Int? = null,
    val skips: PlayerSourceSkips = PlayerSourceSkips.Empty,
)

data class PlayerSourceGraph(
    val balancers: List<PlayerSourceBalancer> = emptyList(),
    val selection: PlayerSourceSelection = PlayerSourceSelection(),
)

data class PlayerSourceBalancer(
    val name: String,
    val dubbings: List<PlayerSourceDubbing> = emptyList(),
)

data class PlayerSourceDubbing(
    val name: String,
    val episodes: List<PlayerSourceEpisode> = emptyList(),
    val views: Int = 0,
)

data class PlayerSourceEpisode(
    val id: Int = 0,
    val number: String = "",
    val iframeUrl: String = "",
    val screenshotUrl: String = "",
    val skips: PlayerSourceSkips = PlayerSourceSkips.Empty,
)

data class PlayerSourceSelection(
    val balancerIndex: Int = 0,
    val dubbingIndex: Int = 0,
    val episodeIndex: Int = 0,
)

data class PlayerSourceSkips(
    val opening: PlayerSourceSkipSegment? = null,
    val ending: PlayerSourceSkipSegment? = null,
) {
    companion object {
        val Empty = PlayerSourceSkips()
    }
}

data class PlayerSourceSkipSegment(
    val startMs: Long,
    val endMs: Long,
)
