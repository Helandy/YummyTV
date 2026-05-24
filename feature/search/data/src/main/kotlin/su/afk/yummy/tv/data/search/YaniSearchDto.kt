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
    @SerialName("blocked_in") val blockedIn: List<String> = emptyList(),
)

@Serializable
data class YaniSearchGenresResponseDto(
    val response: YaniSearchGenresDto = YaniSearchGenresDto(),
)

@Serializable
data class YaniSearchGenresDto(
    val genres: List<YaniSearchGenreDto> = emptyList(),
    val groups: List<YaniSearchGenreGroupDto> = emptyList(),
)

@Serializable
data class YaniSearchGenreDto(
    val title: String = "",
    val value: Int? = null,
    @SerialName("group_id") val groupId: Int? = null,
)

@Serializable
data class YaniSearchGenreGroupDto(
    val title: String = "",
    val id: Int? = null,
)

@Serializable
data class YaniSearchCatalogResponseDto(
    val response: YaniSearchCatalogDto = YaniSearchCatalogDto(),
)

@Serializable
data class YaniSearchCatalogDto(
    val types: List<YaniSearchTypeCountDto> = emptyList(),
)

@Serializable
data class YaniSearchTypeCountDto(
    val type: YaniSearchTypeDto? = null,
)

@Serializable
data class YaniSearchTypeDto(
    val name: String = "",
    val alias: String? = null,
    val value: Int? = null,
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
