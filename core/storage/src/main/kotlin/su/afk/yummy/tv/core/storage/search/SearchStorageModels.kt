package su.afk.yummy.tv.core.storage.search

import su.afk.yummy.tv.core.storage.offlinefirst.isCacheFresh

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
    isCacheFresh(cachedAt, ttlMs)

fun SearchFilterOptionsCache.isFresh(ttlMs: Long): Boolean =
    isCacheFresh(cachedAt, ttlMs)
