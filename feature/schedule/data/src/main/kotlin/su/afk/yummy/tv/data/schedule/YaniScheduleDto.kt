package su.afk.yummy.tv.data.schedule

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YaniScheduleResponseDto(
    val response: List<YaniScheduleAnimeDto> = emptyList(),
)

@Serializable
data class YaniScheduleAnimeDto(
    @SerialName("anime_id") val animeId: Int? = null,
    val title: String = "",
    val poster: YaniSchedulePosterDto? = null,
    val episodes: YaniScheduleEpisodesDto? = null,
)

@Serializable
data class YaniSchedulePosterDto(
    val small: String? = null,
    val medium: String? = null,
    val big: String? = null,
    val fullsize: String? = null,
    val mega: String? = null,
)

@Serializable
data class YaniScheduleEpisodesDto(
    val aired: Int? = null,
    val count: Int? = null,
    @SerialName("next_date") val nextDate: Long? = null,
)
