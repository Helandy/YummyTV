package su.afk.yummy.tv.source.anime4k.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class Anime4kTitleResponseDto(
    val available: Boolean = false,
    val episodes: List<Anime4kEpisodeResponseDto> = emptyList(),
)

@Serializable
data class Anime4kEpisodeResponseDto(
    val number: String = "",
    val season: String = "",
    val dubbings: List<String> = emptyList(),
)

@Serializable
data class Anime4kTicketResponseDto(
    val ticket: String = "",
    val notBeforeSec: Int = 0,
    val expiresInSec: Int = 0,
)

@Serializable
data class Anime4kStreamsRequestDto(
    val ticket: String,
    val malId: Int,
    val season: String,
    val episode: String,
    val team: String,
)

@Serializable
data class Anime4kStreamsResponseDto(
    val referer: String = "",
    val files: List<Anime4kStreamFileDto> = emptyList(),
)

@Serializable
data class Anime4kStreamFileDto(
    val q: Int = 0,
    val br: Int = 0,
    val url: String = "",
)
