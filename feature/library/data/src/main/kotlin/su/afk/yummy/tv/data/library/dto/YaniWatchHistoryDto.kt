package su.afk.yummy.tv.data.library.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YaniWatchHistoryResponseDto(val response: List<YaniWatchHistoryDto> = emptyList())

@Serializable
data class YaniWatchHistoryDto(
    val date: Long = 0,
    @SerialName("end_time") val endTime: Int = 0,
    val duration: Int = 0,
    @SerialName("anime_id") val animeId: Int = 0,
    @SerialName("video_id") val videoId: Int = 0,
    @SerialName("anime_url") val animeUrl: String = "",
    val title: String = "",
    @SerialName("ep_title") val episodeTitle: String = "",
    val episode: String? = null,
    @SerialName("dub_title") val dubbing: String? = null,
    @SerialName("player_title") val player: String? = null,
    val poster: YaniWatchPosterDto? = null,
    val screenshot: YaniWatchScreenshotDto? = null,
)

@Serializable
data class YaniWatchPosterDto(
    val small: String? = null,
    val medium: String? = null,
    val big: String? = null,
    val huge: String? = null,
    val fullsize: String? = null,
    val mega: String? = null,
)

@Serializable
data class YaniWatchScreenshotDto(
    val episode: String? = null,
    val sizes: YaniWatchScreenshotSizesDto? = null,
)

@Serializable
data class YaniWatchScreenshotSizesDto(val small: String? = null, val full: String? = null)
