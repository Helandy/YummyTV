package su.afk.yummy.tv.data.details

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YaniTrailersResponseDto(
    val response: List<YaniTrailerDto> = emptyList(),
)

@Serializable
data class YaniTrailerDto(
    @SerialName("iframe_url") val iframeUrl: String = "",
)
