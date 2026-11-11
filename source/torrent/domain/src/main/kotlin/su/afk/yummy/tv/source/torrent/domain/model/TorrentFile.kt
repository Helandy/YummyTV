package su.afk.yummy.tv.source.torrent.domain.model

/** Файл внутри раздачи; [index] — номер файла в TorrServer (с единицы), [path] — путь внутри раздачи. */
data class TorrentFile(
    val index: Int,
    val path: String,
    val sizeBytes: Long,
)
