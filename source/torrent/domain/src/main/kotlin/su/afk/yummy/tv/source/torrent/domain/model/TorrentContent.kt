package su.afk.yummy.tv.source.torrent.domain.model

/** Раздача, добавленная в TorrServer: [hash] идентифицирует её на сервере, [files] — все файлы раздачи. */
data class TorrentContent(
    val hash: String,
    val title: String,
    val files: List<TorrentFile>,
)
