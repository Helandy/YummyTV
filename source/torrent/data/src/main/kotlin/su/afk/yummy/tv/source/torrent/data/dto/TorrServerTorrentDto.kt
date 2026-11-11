package su.afk.yummy.tv.source.torrent.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Состояние раздачи на TorrServer; `file_stats` пуст, пока метаданные не получены. */
@Serializable
internal data class TorrServerTorrentDto(
    val title: String? = null,
    val hash: String? = null,
    @SerialName("file_stats") val fileStats: List<TorrServerFileDto>? = null,
    @SerialName("active_peers") val activePeers: Int? = null,
    @SerialName("total_peers") val totalPeers: Int? = null,
    @SerialName("connected_seeders") val connectedSeeders: Int? = null,
)

@Serializable
internal data class TorrServerFileDto(
    val id: Int = 0,
    val path: String = "",
    val length: Long = 0L,
)
