package su.afk.yummy.tv.source.torrent.data.source

/** Тексты источника: в data-слое лежат за интерфейсом, чтобы тесты не тянули Android-ресурсы. */
internal interface TorrentMessages {
    /** Название озвучки-пункта и заголовок диалога: «Поиск торрента». */
    val sourceName: String
    val pickTorrentTitle: String
    val pickFileTitle: String
    val cancel: String

    /** Шаг загрузки: запрос в индексатор. */
    val stepSearching: String

    /** Шаг загрузки: TorrServer принимает раздачу и собирает список файлов. */
    val stepFetchingFiles: String

    /** Шаг загрузки со счётчиком пиров: «TorrServer получает список файлов… Пиров: 3 (сидов: 1)». */
    fun stepFetchingFilesPeers(activePeers: Int, totalPeers: Int, trackerSeeders: Int): String
    val notConfigured: String
    val nothingFound: String
    val searchFailed: String
    val serverFailed: String
    val noVideoFiles: String

    /** У раздачи не нашлось пиров, список файлов не получен. */
    val noPeers: String
    val cancelled: String

    /** TorrServer принял раздачу, но данные не пошли (нет доступных пиров). */
    val streamNotStarting: String

    val retry: String

    /** Предупреждение на карточке: у устройства нет декодера HEVC Main10, раздача, скорее всего, не заиграет. */
    val hevcUnsupportedWarning: String

    /** Метка на карточке: раздача уже есть в TorrServer. */
    val inTorrServerBadge: String

    /** Метка «есть субтитры» на карточке. */
    val subtitlesBadge: String

    /** «1,2 ГБ»; пусто, если размер неизвестен. */
    fun sizeText(sizeBytes: Long): String

    /** «15 сидов» с правильным склонением. */
    fun seedersText(seeders: Int): String

    fun fileSubtitle(sizeBytes: Long): String
}
