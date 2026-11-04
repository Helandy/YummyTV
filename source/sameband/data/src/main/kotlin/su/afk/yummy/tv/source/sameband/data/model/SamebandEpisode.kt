package su.afk.yummy.tv.source.sameband.data.model

/** Серия из плейлиста: номер и качества «720p» → абсолютный адрес HLS по возрастанию высоты. */
internal data class SamebandEpisode(
    val number: Int,
    val qualities: Map<String, String>,
)
