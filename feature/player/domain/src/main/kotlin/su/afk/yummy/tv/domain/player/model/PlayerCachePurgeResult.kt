package su.afk.yummy.tv.domain.player.model

/**
 * Итог очистки кэша воспроизведения.
 *
 * @property entries сколько закэшированных ресурсов (файлов, манифестов, сегментов) удалено.
 * @property bytes сколько байт освобождено на диске.
 * @property largest самые крупные из удалённых ресурсов, по убыванию размера: этого достаточно,
 *   чтобы понять по логу, какой файл и какого качества лежал в кэше.
 */
data class PlayerCachePurgeResult(
    val entries: Int,
    val bytes: Long,
    val largest: List<PlayerCachePurgedEntry> = emptyList(),
)
