package su.afk.yummy.tv.core.storage.search

class SearchStorageStore(private val dao: SearchStorageDao) {

    suspend fun getPage(pageKey: String): SearchPageCache? =
        dao.getPage(pageKey)

    suspend fun savePage(cache: SearchPageCache) {
        dao.replacePage(cache)
    }

    suspend fun getFilterOptions(language: String): SearchFilterOptionsCache? =
        dao.getFilterOptions(language)

    suspend fun saveFilterOptions(cache: SearchFilterOptionsCache) {
        dao.replaceFilterOptions(cache)
    }
}
