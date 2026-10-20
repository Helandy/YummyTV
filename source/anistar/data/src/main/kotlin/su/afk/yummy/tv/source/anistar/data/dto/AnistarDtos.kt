package su.afk.yummy.tv.source.anistar.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AnistarLookupRequestDto(
    val malId: Int,
    val title: String,
    val otherTitles: List<String>,
    val year: Int? = null,
    val type: String? = null,
    val episodes: Int? = null,
)

@Serializable
data class AnistarTitleResponseDto(
    val available: Boolean = false,
    val newsId: Long? = null,
    val mirror: String? = null,
)

@Serializable
data class AnistarMirrorResponseDto(
    val mirror: String? = null,
)
