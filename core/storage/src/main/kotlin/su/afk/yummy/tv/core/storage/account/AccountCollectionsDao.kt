package su.afk.yummy.tv.core.storage.account

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class AccountCollectionsDao {

    @Query("SELECT * FROM account_collection_pages WHERE pageKey = :pageKey LIMIT 1")
    abstract suspend fun getCollectionPageEntry(pageKey: String): AccountCollectionPageEntry?

    @Query("SELECT * FROM account_collection_items WHERE pageKey = :pageKey ORDER BY position")
    abstract suspend fun getCollectionItems(pageKey: String): List<AccountCollectionItemEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCollectionPage(entry: AccountCollectionPageEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCollectionItems(entries: List<AccountCollectionItemEntry>)

    @Query("DELETE FROM account_collection_pages WHERE pageKey = :pageKey")
    abstract suspend fun deleteCollectionPage(pageKey: String)

    @Query("DELETE FROM account_collection_items WHERE pageKey = :pageKey")
    abstract suspend fun deleteCollectionItems(pageKey: String)

    @Query(
        """
        DELETE FROM account_collection_items
        WHERE pageKey IN (
            SELECT pageKey FROM account_collection_pages WHERE cachedAt < :minCachedAt
        )
        """
    )
    abstract suspend fun deleteCollectionItemsCachedBefore(minCachedAt: Long)

    @Query("DELETE FROM account_collection_pages WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteCollectionPagesCachedBefore(minCachedAt: Long)

    @Query("DELETE FROM account_collection_pages")
    abstract suspend fun deleteAllCollectionPages()

    @Query("DELETE FROM account_collection_items")
    abstract suspend fun deleteAllCollectionItems()

    @Transaction
    open suspend fun getCollections(pageKey: String): AccountCollectionsPageCache? {
        val entry = getCollectionPageEntry(pageKey) ?: return null
        return AccountCollectionsPageCache(entry, getCollectionItems(pageKey))
    }

    @Transaction
    open suspend fun replaceCollections(
        cache: AccountCollectionsPageCache,
        prunePagesCachedBefore: Long? = null,
    ) {
        val pageKey = cache.entry.pageKey
        deleteCollectionPage(pageKey)
        deleteCollectionItems(pageKey)
        insertCollectionPage(cache.entry)
        if (cache.items.isNotEmpty()) insertCollectionItems(cache.items)

        prunePagesCachedBefore?.let {
            deleteCollectionItemsCachedBefore(it)
            deleteCollectionPagesCachedBefore(it)
        }
    }

    @Transaction
    open suspend fun invalidateCollections() {
        deleteAllCollectionItems()
        deleteAllCollectionPages()
    }
}
