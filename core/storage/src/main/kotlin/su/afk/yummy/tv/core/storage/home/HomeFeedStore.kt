package su.afk.yummy.tv.core.storage.home

class HomeFeedStore(private val dao: HomeFeedDao) {

    suspend fun getFeed(
        language: String,
        watchSignature: String = HOME_FEED_GENERIC_WATCH_SIGNATURE,
    ): HomeFeedCache? =
        dao.getFeed(language, watchSignature)

    suspend fun saveFeed(cache: HomeFeedCache) {
        dao.replaceFeed(cache)
    }
}
