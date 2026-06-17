package su.afk.yummy.tv.core.storage.collection

data class CollectionDetailCache(
    val entry: CollectionDetailEntry,
    val items: List<CollectionAnimeItemEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

fun CollectionDetailCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs

data class CollectionCatalogPageCache(
    val entry: CollectionCatalogPageEntry,
    val items: List<CollectionCatalogItemEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

fun CollectionCatalogPageCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs
