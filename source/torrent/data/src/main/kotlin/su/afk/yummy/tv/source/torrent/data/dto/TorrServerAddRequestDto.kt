package su.afk.yummy.tv.source.torrent.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Тело `POST /torrents`: `action` = `add` (добавить) или `get` (прочитать состояние по [hash]). */
@Serializable
internal data class TorrServerAddRequestDto(
    val action: String,
    val link: String? = null,
    val hash: String? = null,
    @SerialName("save_to_db") val saveToDb: Boolean = false,
)
