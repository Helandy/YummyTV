package su.afk.yummy.tv.core.storage.collection

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class CollectionStorageDao {

    @Query(
        """
        SELECT * FROM collection_details
        WHERE collectionId = :collectionId AND language = :language
        LIMIT 1
        """
    )
    abstract suspend fun getDetailEntry(collectionId: Int, language: String): CollectionDetailEntry?

    @Query(
        """
        SELECT * FROM collection_anime_items
        WHERE collectionId = :collectionId AND language = :language
        ORDER BY position
        """
    )
    abstract suspend fun getItems(
        collectionId: Int,
        language: String
    ): List<CollectionAnimeItemEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDetail(entry: CollectionDetailEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertItems(entries: List<CollectionAnimeItemEntry>)

    @Query("DELETE FROM collection_details WHERE collectionId = :collectionId AND language = :language")
    abstract suspend fun deleteDetail(collectionId: Int, language: String)

    @Query("DELETE FROM collection_anime_items WHERE collectionId = :collectionId AND language = :language")
    abstract suspend fun deleteItems(collectionId: Int, language: String)

    @Query("DELETE FROM collection_details WHERE collectionId = :collectionId")
    abstract suspend fun deleteDetailsForCollection(collectionId: Int)

    @Query("DELETE FROM collection_anime_items WHERE collectionId = :collectionId")
    abstract suspend fun deleteItemsForCollection(collectionId: Int)

    @Query(
        """
        UPDATE collection_details
        SET likes = :likes,
            dislikes = :dislikes,
            vote = :vote
        WHERE collectionId = :collectionId AND language = :language
        """
    )
    abstract suspend fun updateDetailVote(
        collectionId: Int,
        language: String,
        likes: Int,
        dislikes: Int,
        vote: Int,
    )

    @Query("SELECT * FROM collection_catalog_pages WHERE pageKey = :pageKey LIMIT 1")
    abstract suspend fun getCatalogPageEntry(pageKey: String): CollectionCatalogPageEntry?

    @Query("SELECT * FROM collection_catalog_items WHERE pageKey = :pageKey ORDER BY position")
    abstract suspend fun getCatalogItems(pageKey: String): List<CollectionCatalogItemEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCatalogPage(entry: CollectionCatalogPageEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCatalogItems(entries: List<CollectionCatalogItemEntry>)

    @Query("DELETE FROM collection_catalog_pages WHERE pageKey = :pageKey")
    abstract suspend fun deleteCatalogPage(pageKey: String)

    @Query("DELETE FROM collection_catalog_items WHERE pageKey = :pageKey")
    abstract suspend fun deleteCatalogItems(pageKey: String)

    @Query("DELETE FROM collection_catalog_pages")
    abstract suspend fun deleteAllCatalogPages()

    @Query("DELETE FROM collection_catalog_items")
    abstract suspend fun deleteAllCatalogItems()

    @Transaction
    open suspend fun getCollection(collectionId: Int, language: String): CollectionDetailCache? {
        val entry = getDetailEntry(collectionId, language) ?: return null
        return CollectionDetailCache(
            entry = entry,
            items = getItems(collectionId, language),
        )
    }

    @Transaction
    open suspend fun replaceCollection(cache: CollectionDetailCache) {
        val collectionId = cache.entry.collectionId
        val language = cache.entry.language
        deleteDetail(collectionId, language)
        deleteItems(collectionId, language)

        insertDetail(cache.entry)
        if (cache.items.isNotEmpty()) insertItems(cache.items)
    }

    @Transaction
    open suspend fun deleteCollection(collectionId: Int) {
        deleteItemsForCollection(collectionId)
        deleteDetailsForCollection(collectionId)
    }

    @Transaction
    open suspend fun invalidateCatalog() {
        deleteAllCatalogItems()
        deleteAllCatalogPages()
    }

    @Transaction
    open suspend fun getCatalogPage(pageKey: String): CollectionCatalogPageCache? {
        val entry = getCatalogPageEntry(pageKey) ?: return null
        return CollectionCatalogPageCache(
            entry = entry,
            items = getCatalogItems(pageKey),
        )
    }

    @Transaction
    open suspend fun replaceCatalogPage(cache: CollectionCatalogPageCache) {
        val pageKey = cache.entry.pageKey
        deleteCatalogPage(pageKey)
        deleteCatalogItems(pageKey)

        insertCatalogPage(cache.entry)
        if (cache.items.isNotEmpty()) insertCatalogItems(cache.items)
    }
}
