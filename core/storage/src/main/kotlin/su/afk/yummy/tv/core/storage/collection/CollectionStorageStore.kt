package su.afk.yummy.tv.core.storage.collection

internal class CollectionStorageStore(private val dao: CollectionStorageDao) : CollectionStorage {

    override suspend fun getCollection(
        collectionId: Int,
        language: String
    ): CollectionDetailCache? =
        dao.getCollection(collectionId, language)

    override suspend fun saveCollection(cache: CollectionDetailCache) {
        dao.replaceCollection(cache)
    }

    override suspend fun updateCollectionVote(
        collectionId: Int,
        language: String,
        likes: Int,
        dislikes: Int,
        vote: Int,
    ) {
        dao.updateDetailVote(collectionId, language, likes, dislikes, vote)
    }

    override suspend fun deleteCollection(collectionId: Int) {
        dao.deleteCollection(collectionId)
    }

    override suspend fun invalidateCatalog() {
        dao.expireCatalogPages()
    }

    override suspend fun getCatalogPage(pageKey: String): CollectionCatalogPageCache? =
        dao.getCatalogPage(pageKey)

    override suspend fun saveCatalogPage(cache: CollectionCatalogPageCache) {
        dao.replaceCatalogPage(cache)
    }
}
