package su.afk.yummy.tv.core.storage.collection

class CollectionStorageStore(private val dao: CollectionStorageDao) {

    suspend fun getCollection(collectionId: Int, language: String): CollectionDetailCache? =
        dao.getCollection(collectionId, language)

    suspend fun saveCollection(cache: CollectionDetailCache) {
        dao.replaceCollection(cache)
    }

    suspend fun updateCollectionVote(
        collectionId: Int,
        language: String,
        likes: Int,
        dislikes: Int,
        vote: Int,
    ) {
        dao.updateDetailVote(collectionId, language, likes, dislikes, vote)
    }

    suspend fun deleteCollection(collectionId: Int) {
        dao.deleteCollection(collectionId)
    }

    suspend fun invalidateCatalog() {
        dao.invalidateCatalog()
    }

    suspend fun getCatalogPage(pageKey: String): CollectionCatalogPageCache? =
        dao.getCatalogPage(pageKey)

    suspend fun saveCatalogPage(cache: CollectionCatalogPageCache) {
        dao.replaceCatalogPage(cache)
    }
}
