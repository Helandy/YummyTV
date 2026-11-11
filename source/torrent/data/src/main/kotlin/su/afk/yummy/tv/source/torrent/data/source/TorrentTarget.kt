package su.afk.yummy.tv.source.torrent.data.source

/** Что ищем: тайтл [animeId], серия [episode] и названия для поиска [queries] (русское, латинское). */
internal data class TorrentTarget(
    val animeId: Int,
    val episode: String,
    val queries: List<String>,
)
