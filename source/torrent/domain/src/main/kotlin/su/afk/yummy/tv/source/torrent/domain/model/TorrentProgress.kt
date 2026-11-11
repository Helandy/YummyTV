package su.afk.yummy.tv.source.torrent.domain.model

/** Как идёт поиск пиров у раздачи на TorrServer: [activePeers] подключено, [totalPeers] известно, [seeders] из них сидов. */
data class TorrentProgress(
    val activePeers: Int,
    val totalPeers: Int,
    val seeders: Int,
)
