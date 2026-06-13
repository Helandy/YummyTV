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
}
