package su.afk.yummy.tv.data.collection.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YaniCollectionDetailResponseDto(
    val response: YaniCollectionDetailDto = YaniCollectionDetailDto(),
)

@Serializable
data class YaniCollectionListResponseDto(
    val response: List<YaniCollectionDetailDto> = emptyList(),
)

@Serializable
data class YaniCreateCollectionBodyDto(
    @SerialName("public") val isPublic: Boolean,
    @SerialName("anime_ids") val animeIds: List<Int> = emptyList(),
    val language: String,
    val description: String,
    val title: String,
)

@Serializable
data class YaniCreateCollectionResponseDto(
    val response: YaniCreateCollectionPayloadDto = YaniCreateCollectionPayloadDto(),
)

@Serializable
data class YaniCreateCollectionPayloadDto(
    val id: Int = 0,
)

@Serializable
data class YaniCollectionDetailDto(
    val id: Int? = null,
    val owner: YaniCollectionOwnerDto? = null,
    val title: String = "",
    val description: String = "",
    @SerialName("public") val isPublic: Boolean = false,
    val views: Int = 0,
    val likes: YaniCollectionLikesDto? = null,
    val animes: List<YaniCollectionAnimeDto> = emptyList(),
    @SerialName("poster_previews") val posterPreviews: List<YaniCollectionPosterDto> = emptyList(),
)

@Serializable
data class YaniCollectionOwnerDto(
    val id: Int? = null,
)

@Serializable
data class YaniUpdateCollectionBodyDto(
    @SerialName("public") val isPublic: Boolean,
    val description: String,
    val title: String,
)

@Serializable
data class YaniCollectionMutationResponseDto(
    val response: Boolean = false,
)

@Serializable
data class YaniCollectionLikesDto(
    val likes: Int = 0,
    val dislikes: Int = 0,
    val vote: Int = 0,
)

@Serializable
data class YaniCollectionVoteBodyDto(
    val action: Int,
)

@Serializable
data class YaniCollectionVoteResponseDto(
    val response: YaniCollectionVotePayloadDto = YaniCollectionVotePayloadDto(),
)

@Serializable
data class YaniCollectionVotePayloadDto(
    val likes: Int = 0,
    val dislikes: Int = 0,
)

@Serializable
data class YaniCollectionAnimeDto(
    @SerialName("anime_id") val animeId: Int? = null,
    val title: String = "",
    val poster: YaniCollectionPosterDto? = null,
    val rating: YaniCollectionRatingDto? = null,
    val year: Int? = null,
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
