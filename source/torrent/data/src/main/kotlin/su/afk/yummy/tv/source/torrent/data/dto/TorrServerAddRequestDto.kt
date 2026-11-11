package su.afk.yummy.tv.source.torrent.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Тело `POST /torrents`: `action` = `add` (добавить), `get` (прочитать состояние по [hash]) или `list` (все раздачи сервера). */
@Serializable
internal data class TorrServerAddRequestDto(
    val action: String,
    val link: String? = null,
    val title: String? = null,
    val hash: String? = null,
    @SerialName("save_to_db") val saveToDb: Boolean = false,
)
