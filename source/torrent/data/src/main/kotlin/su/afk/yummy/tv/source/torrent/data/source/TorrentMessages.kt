package su.afk.yummy.tv.source.torrent.data.source

/** Тексты источника: в data-слое лежат за интерфейсом, чтобы тесты не тянули Android-ресурсы. */
internal interface TorrentMessages {
    /** Название озвучки-пункта и заголовок диалога: «Поиск торрента». */
    val sourceName: String
    val pickTorrentTitle: String
    val pickFileTitle: String
    val cancel: String
    val notConfigured: String
    val nothingFound: String
    val searchFailed: String
    val serverFailed: String
    val noVideoFiles: String
    val cancelled: String

    /** «1,2 ГБ · 15 сидов · rutracker» — строка раздачи в списке. */
    fun torrentSubtitle(sizeBytes: Long, seeders: Int, tracker: String): String

    fun fileSubtitle(sizeBytes: Long): String
}
