package su.afk.yummy.tv.data.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YaniSearchResponseDto(
    val response: List<YaniSearchItemDto> = emptyList(),
)

@Serializable
data class YaniSearchItemDto(
    @SerialName("anime_id") val animeId: Int? = null,
    val title: String = "",
    val poster: YaniSearchPosterDto? = null,
    val rating: YaniSearchRatingDto? = null,
)

@Serializable
data class YaniSearchPosterDto(
    val small: String? = null,
    val medium: String? = null,
    val big: String? = null,
    val fullsize: String? = null,
)

@Serializable
data class YaniSearchRatingDto(
    val average: Double? = null,
)
