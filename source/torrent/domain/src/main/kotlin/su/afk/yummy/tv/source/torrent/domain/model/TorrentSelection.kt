package su.afk.yummy.tv.source.torrent.domain.model

/** Итог выбора пользователя: файл [file] раздачи [hash], которую нужно играть через TorrServer. */
data class TorrentSelection(
    val hash: String,
    val title: String,
    val file: TorrentFile,
)
