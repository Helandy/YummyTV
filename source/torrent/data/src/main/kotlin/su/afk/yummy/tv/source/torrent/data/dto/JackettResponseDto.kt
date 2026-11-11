package su.afk.yummy.tv.source.torrent.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Ответ Jackett `/api/v2.0/indexers/all/results`. */
@Serializable
internal data class JackettResponseDto(
    @SerialName("Results") val results: List<JackettResultDto> = emptyList(),
)

@Serializable
internal data class JackettResultDto(
    @SerialName("Tracker") val tracker: String? = null,
    @SerialName("Title") val title: String? = null,
    @SerialName("Size") val size: Long? = null,
    @SerialName("Seeders") val seeders: Int? = null,
    @SerialName("Peers") val peers: Int? = null,
    @SerialName("MagnetUri") val magnetUri: String? = null,
    @SerialName("Link") val link: String? = null,
)
