package su.afk.yummy.tv.core.storage.schedule

data class AnimeScheduleCache(
    val entry: AnimeScheduleCacheEntry,
    val items: List<AnimeScheduleItemEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

fun AnimeScheduleCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs
