package su.afk.yummy.tv.source.amedia.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AmediaLookupRequestDto(
    val malId: Int,
    val title: String,
    val otherTitles: List<String>,
    val year: Int? = null,
    val type: String? = null,
    val episodes: Int? = null,
)

@Serializable
data class AmediaTitleResponseDto(
    val available: Boolean = false,
    val pageUrl: String? = null,
)
