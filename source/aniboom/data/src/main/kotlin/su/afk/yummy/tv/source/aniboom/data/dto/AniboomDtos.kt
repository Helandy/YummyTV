package su.afk.yummy.tv.source.aniboom.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AniboomLookupRequestDto(
    val malId: Int,
    val title: String,
    val otherTitles: List<String>,
    val year: Int? = null,
    val type: String? = null,
    val episodes: Int? = null,
)

@Serializable
data class AniboomTitleResponseDto(
    val available: Boolean = false,
    val aniboomId: String? = null,
    val episodesAvailable: Int = 0,
    val dubbings: List<AniboomDubbingResponseDto> = emptyList(),
)

@Serializable
data class AniboomDubbingResponseDto(
    val title: String = "",
    val translation: Int = 0,
)
