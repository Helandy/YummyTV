package su.afk.yummy.tv.data.library.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniTransferUserListResponseDto(
    val response: List<YaniTransferUserAnimeDto> = emptyList(),
)

@Serializable
data class YaniTransferUserAnimeDto(
    @SerialName("anime_id") val animeId: Int? = null,
    val title: String = "",
    val type: YaniTransferTypeDto? = null,
    @SerialName("remote_ids") val remoteIds: YaniTransferRemoteIdsDto? = null,
    val user: YaniTransferUserStateDto? = null,
)

@Serializable
data class YaniTransferLookupResponseDto(
    val response: List<YaniTransferLookupAnimeDto> = emptyList(),
)

@Serializable
data class YaniTransferLookupAnimeDto(
    @SerialName("anime_id") val animeId: Int? = null,
    val title: String = "",
    val poster: YaniTransferPosterDto? = null,
    val rating: YaniTransferRatingDto? = null,
    val year: Int? = null,
    /** В ответах yani приходит числом `1..4` или слагом сезона — разбирается вручную. */
    val season: JsonElement? = null,
    val type: YaniTransferTypeDto? = null,
    @SerialName("anime_status") val animeStatus: YaniTransferStatusDto? = null,
    @SerialName("remote_ids") val remoteIds: YaniTransferRemoteIdsDto? = null,
)

@Serializable
data class YaniTransferRemoteIdsDto(
    @SerialName("shikimori_id") val shikimoriId: Int? = null,
    @SerialName("myanimelist_id") val myAnimeListId: Int? = null,
)

@Serializable
data class YaniTransferPosterDto(
    val small: String? = null,
    val medium: String? = null,
    val big: String? = null,
    val fullsize: String? = null,
    val mega: String? = null,
)

@Serializable
data class YaniTransferRatingDto(
    val average: Double? = null,
)

@Serializable
data class YaniTransferTypeDto(
    val name: String? = null,
    @SerialName("shortname") val shortName: String? = null,
    val alias: String? = null,
)

@Serializable
data class YaniTransferStatusDto(
    val alias: String? = null,
)

@Serializable
data class YaniTransferUserStateDto(
    val list: YaniTransferUserListDto? = null,
    val rating: Int? = null,
)

@Serializable
data class YaniTransferUserListDto(
    @SerialName("is_fav") val isFav: Boolean = false,
    val list: YaniTransferListInfoDto? = null,
)

@Serializable
data class YaniTransferListInfoDto(
    val id: Int? = null,
)
