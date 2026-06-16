package su.afk.yummy.tv.core.storage.home

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class HomeFeedDao {

    @Query(
        """
        SELECT * FROM home_feed_caches
        WHERE language = :language AND watchSignature = :watchSignature
        LIMIT 1
        """
    )
    abstract suspend fun getCacheEntry(
        language: String,
        watchSignature: String,
    ): HomeFeedCacheEntry?

    @Query(
        """
        SELECT * FROM home_feed_items
        WHERE language = :language AND watchSignature = :watchSignature
        ORDER BY container, position
        """
    )
    abstract suspend fun getItems(
        language: String,
        watchSignature: String,
    ): List<HomeFeedItemEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCache(entry: HomeFeedCacheEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertItems(entries: List<HomeFeedItemEntry>)

    @Query(
        """
        DELETE FROM home_feed_caches
        WHERE language = :language AND watchSignature = :watchSignature
        """
    )
    abstract suspend fun deleteCache(
        language: String,
        watchSignature: String,
    )

    @Query(
        """
        DELETE FROM home_feed_items
        WHERE language = :language AND watchSignature = :watchSignature
        """
    )
    abstract suspend fun deleteItems(
        language: String,
        watchSignature: String,
    )

    @Transaction
    open suspend fun getFeed(
        language: String,
        watchSignature: String,
    ): HomeFeedCache? {
        val entry = getCacheEntry(language, watchSignature) ?: return null
        return HomeFeedCache(
            entry = entry,
            items = getItems(language, watchSignature),
        )
    }

    @Transaction
    open suspend fun replaceFeed(cache: HomeFeedCache) {
        val language = cache.entry.language
        val watchSignature = cache.entry.watchSignature
        deleteCache(language, watchSignature)
        deleteItems(language, watchSignature)

        insertCache(cache.entry)
        if (cache.items.isNotEmpty()) insertItems(cache.items)
    }
}
