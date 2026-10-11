package su.afk.yummy.tv.domain.player.model

/**
 * Один ресурс, выброшенный из кэша воспроизведения.
 *
 * @property key ключ кэша; query-часть у не-okcdn ключей срезана, чтобы подписи и токены не
 *   попадали в лог.
 * @property cachedBytes сколько байт ресурса лежало в кэше.
 * @property contentLength полный размер ресурса, если он известен кэшу, иначе -1.
 * @property spans из скольких кусков состояла закэшированная часть.
 * @property fromPosition смещение начала самого раннего закэшированного куска.
 * @property toPosition смещение конца самого позднего закэшированного куска.
 */
data class PlayerCachePurgedEntry(
    val key: String,
    val cachedBytes: Long,
    val contentLength: Long,
    val spans: Int,
    val fromPosition: Long,
    val toPosition: Long,
)
