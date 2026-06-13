package su.afk.yummy.tv.core.storage.home

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class HomeFeedDao {

    @Query("SELECT * FROM home_feed_caches WHERE language = :language LIMIT 1")
    abstract suspend fun getCacheEntry(language: String): HomeFeedCacheEntry?

    @Query("SELECT * FROM home_feed_items WHERE language = :language ORDER BY container, position")
    abstract suspend fun getItems(language: String): List<HomeFeedItemEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCache(entry: HomeFeedCacheEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertItems(entries: List<HomeFeedItemEntry>)

    @Query("DELETE FROM home_feed_caches WHERE language = :language")
    abstract suspend fun deleteCache(language: String)

    @Query("DELETE FROM home_feed_items WHERE language = :language")
    abstract suspend fun deleteItems(language: String)

    @Transaction
    open suspend fun getFeed(language: String): HomeFeedCache? {
        val entry = getCacheEntry(language) ?: return null
        return HomeFeedCache(
            entry = entry,
            items = getItems(language),
        )
    }

    @Transaction
    open suspend fun replaceFeed(cache: HomeFeedCache) {
        val language = cache.entry.language
        deleteCache(language)
        deleteItems(language)

        insertCache(cache.entry)
        if (cache.items.isNotEmpty()) insertItems(cache.items)
    }
}
