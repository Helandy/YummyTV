package su.afk.yummy.tv.source.availability.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AvailabilityRequestDto(
    val titles: List<String>,
    val malId: Int? = null,
    val year: Int? = null,
    val type: String? = null,
    val episodes: Int? = null,
)

@Serializable
data class AvailabilityResponseDto(
    val sources: List<SourceAvailabilityDto> = emptyList(),
    val failed: List<String> = emptyList(),
)

@Serializable
data class SourceAvailabilityDto(
    val sourceId: String = "",
    val key: String = "",
    val url: String = "",
    val episodesAvailable: Int? = null,
    val episodesTotal: Int? = null,
    val ongoing: Boolean = false,
    val qualities: List<String> = emptyList(),
    val variants: List<SourceVariantDto> = emptyList(),
    val mirrors: List<String> = emptyList(),
)

@Serializable
data class SourceVariantDto(
    val key: String = "",
    val kind: String? = null,
    val title: String? = null,
    val episodes: List<SourceEpisodeDto> = emptyList(),
)

@Serializable
data class SourceEpisodeDto(
    val number: Int = 0,
    val durationSec: Int? = null,
    val openingStartSec: Int? = null,
    val openingEndSec: Int? = null,
    val id: String? = null,
    val url: String? = null,
)
