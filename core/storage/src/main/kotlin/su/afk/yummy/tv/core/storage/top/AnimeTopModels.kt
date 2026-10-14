package su.afk.yummy.tv.core.storage.top

import su.afk.yummy.tv.core.storage.offlinefirst.isCacheFresh

data class AnimeTopPageCache(
    val entry: AnimeTopPageEntry,
    val items: List<AnimeTopItemEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

fun AnimeTopPageCache.isFresh(ttlMs: Long): Boolean =
    isCacheFresh(cachedAt, ttlMs)
