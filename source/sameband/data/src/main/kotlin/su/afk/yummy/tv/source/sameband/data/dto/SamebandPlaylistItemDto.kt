package su.afk.yummy.tv.source.sameband.data.dto

import kotlinx.serialization.Serializable

/** Элемент плейлиста плеера: `title` — HTML с подписью «Серия NN», `file` — `[480p]/v/…m3u8,[720p]…`. */
@Serializable
internal data class SamebandPlaylistItemDto(
    val title: String? = null,
    val file: String? = null,
)
