package su.afk.yummy.tv.core.storage.collection

data class CollectionDetailCache(
    val entry: CollectionDetailEntry,
    val items: List<CollectionAnimeItemEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

fun CollectionDetailCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs
