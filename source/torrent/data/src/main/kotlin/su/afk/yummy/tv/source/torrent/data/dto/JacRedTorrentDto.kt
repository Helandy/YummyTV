package su.afk.yummy.tv.source.torrent.data.dto

import kotlinx.serialization.Serializable

/** Раздача из JacRed (`/api/v1.0/torrents`); все поля необязательны: индексаторы заполняют их по-разному. */
@Serializable
internal data class JacRedTorrentDto(
    val tracker: String? = null,
    val url: String? = null,
    val title: String? = null,
    val sid: Int? = null,
    val pir: Int? = null,
    val size: Long? = null,
    val magnet: String? = null,
)
