package su.afk.yummy.tv.core.storage.schedule

import su.afk.yummy.tv.core.storage.offlinefirst.isCacheFresh

data class AnimeScheduleCache(
    val entry: AnimeScheduleCacheEntry,
    val items: List<AnimeScheduleItemEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

fun AnimeScheduleCache.isFresh(ttlMs: Long): Boolean =
    isCacheFresh(cachedAt, ttlMs)
