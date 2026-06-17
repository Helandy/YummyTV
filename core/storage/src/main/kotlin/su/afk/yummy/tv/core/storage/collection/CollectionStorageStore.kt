package su.afk.yummy.tv.core.storage.collection

class CollectionStorageStore(private val dao: CollectionStorageDao) {

    suspend fun getCollection(collectionId: Int, language: String): CollectionDetailCache? =
        dao.getCollection(collectionId, language)

    suspend fun saveCollection(cache: CollectionDetailCache) {
        dao.replaceCollection(cache)
    }

    suspend fun getCatalogPage(pageKey: String): CollectionCatalogPageCache? =
        dao.getCatalogPage(pageKey)

    suspend fun saveCatalogPage(cache: CollectionCatalogPageCache) {
        dao.replaceCatalogPage(cache)
    }
}
