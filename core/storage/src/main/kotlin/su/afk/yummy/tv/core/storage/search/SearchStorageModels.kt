package su.afk.yummy.tv.core.storage.search

data class SearchPageCache(
    val entry: SearchPageEntry,
    val items: List<SearchItemEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

data class SearchFilterOptionsCache(
    val entry: SearchFilterOptionsEntry,
    val genreGroups: List<SearchGenreGroupEntry>,
    val genres: List<SearchGenreEntry>,
    val types: List<SearchTypeEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

fun SearchPageCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs

fun SearchFilterOptionsCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs
