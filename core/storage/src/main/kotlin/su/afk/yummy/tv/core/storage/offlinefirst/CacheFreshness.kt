package su.afk.yummy.tv.core.storage.offlinefirst

/**
 * Единственное место, где решается «кэш ещё свежий»: запись, сохранённая в [cachedAt], живёт [ttlMs].
 *
 * Все `isFresh(ttlMs)` кэш-моделей сводятся сюда, поэтому источник времени и правило границы
 * (строго `<`) меняются в одной точке. [nowMs] можно подменить, когда нужно проверить границу TTL
 * без реальных часов.
 */
fun isCacheFresh(
    cachedAt: Long,
    ttlMs: Long,
    nowMs: Long = System.currentTimeMillis(),
): Boolean = nowMs - cachedAt < ttlMs
