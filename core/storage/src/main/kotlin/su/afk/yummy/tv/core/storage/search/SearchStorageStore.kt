package su.afk.yummy.tv.core.storage.search

class SearchStorageStore(private val dao: SearchStorageDao) {

    suspend fun getPage(pageKey: String): SearchPageCache? =
        dao.getPage(pageKey)

    suspend fun savePage(
        cache: SearchPageCache,
        prunePagesCachedBefore: Long? = null,
    ) {
        dao.replacePage(cache, prunePagesCachedBefore)
    }

    suspend fun getFilterOptions(language: String): SearchFilterOptionsCache? =
        dao.getFilterOptions(language)

    suspend fun saveFilterOptions(cache: SearchFilterOptionsCache) {
        dao.replaceFilterOptions(cache)
    }
}
