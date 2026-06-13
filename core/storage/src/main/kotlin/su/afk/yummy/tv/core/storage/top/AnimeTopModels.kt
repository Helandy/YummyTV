package su.afk.yummy.tv.core.storage.top

data class AnimeTopPageCache(
    val entry: AnimeTopPageEntry,
    val items: List<AnimeTopItemEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

fun AnimeTopPageCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs
