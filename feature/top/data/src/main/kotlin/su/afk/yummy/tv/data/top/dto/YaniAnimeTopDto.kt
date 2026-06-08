package su.afk.yummy.tv.data.top.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YaniAnimeTopListDto(
    val response: List<YaniAnimeTopItemDto> = emptyList(),
)

@Serializable
data class YaniAnimeTopItemDto(
    @SerialName("anime_id") val animeId: Int? = null,
    val title: String = "",
    val poster: YaniAnimeTopPosterDto? = null,
    val rating: YaniAnimeTopRatingDto? = null,
)

@Serializable
data class YaniAnimeTopPosterDto(
    val small: String? = null,
    val medium: String? = null,
    val big: String? = null,
    val fullsize: String? = null,
)

@Serializable
data class YaniAnimeTopRatingDto(
    val average: Double? = null,
)
