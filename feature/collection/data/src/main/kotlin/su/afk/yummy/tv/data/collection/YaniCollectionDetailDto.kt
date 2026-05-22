package su.afk.yummy.tv.data.collection

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YaniCollectionDetailResponseDto(
    val response: YaniCollectionDetailDto = YaniCollectionDetailDto(),
)

@Serializable
data class YaniCollectionDetailDto(
    val id: Int? = null,
    val title: String = "",
    val description: String = "",
    val views: Int = 0,
    val animes: List<YaniCollectionAnimeDto> = emptyList(),
    @SerialName("poster_previews") val posterPreviews: List<YaniCollectionPosterDto> = emptyList(),
)

@Serializable
data class YaniCollectionAnimeDto(
    @SerialName("anime_id") val animeId: Int? = null,
    val title: String = "",
    val poster: YaniCollectionPosterDto? = null,
    val rating: YaniCollectionRatingDto? = null,
)

@Serializable
data class YaniCollectionPosterDto(
    val small: String? = null,
    val medium: String? = null,
    val big: String? = null,
    val fullsize: String? = null,
)

@Serializable
data class YaniCollectionRatingDto(
    val average: Double? = null,
)
