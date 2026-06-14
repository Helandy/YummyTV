package su.afk.yummy.tv.core.storage.top

class AnimeTopStore(private val dao: AnimeTopDao) {

    suspend fun getPage(
        type: String,
        language: String,
        limit: Int,
        offset: Int,
    ): AnimeTopPageCache? =
        dao.getPage(type, language, limit, offset)

    suspend fun savePage(
        cache: AnimeTopPageCache,
        prunePagesCachedBefore: Long? = null,
    ) {
        dao.replacePage(cache, prunePagesCachedBefore)
    }
}
