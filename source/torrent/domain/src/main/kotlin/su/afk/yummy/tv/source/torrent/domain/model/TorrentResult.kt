package su.afk.yummy.tv.source.torrent.domain.model

/**
 * Раздача из индексатора. [link] — то, что принимает TorrServer: magnet-ссылка или адрес `.torrent`-файла.
 */
data class TorrentResult(
    val title: String,
    val sizeBytes: Long,
    val seeders: Int,
    val peers: Int,
    val tracker: String,
    val link: String,
)
