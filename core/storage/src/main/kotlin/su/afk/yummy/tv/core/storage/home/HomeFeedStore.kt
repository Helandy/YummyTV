package su.afk.yummy.tv.core.storage.home

class HomeFeedStore(private val dao: HomeFeedDao) {

    suspend fun getFeed(language: String): HomeFeedCache? =
        dao.getFeed(language)

    suspend fun saveFeed(cache: HomeFeedCache) {
        dao.replaceFeed(cache)
    }
}
