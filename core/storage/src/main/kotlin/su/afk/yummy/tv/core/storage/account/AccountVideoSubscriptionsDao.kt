package su.afk.yummy.tv.core.storage.account

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class AccountVideoSubscriptionsDao {

    @Query(
        """
        SELECT * FROM account_video_subscription_caches
        WHERE userId = :userId AND language = :language
        LIMIT 1
        """
    )
    abstract suspend fun getVideoSubscriptionCacheEntry(
        userId: Int,
        language: String,
    ): AccountVideoSubscriptionCacheEntry?

    @Query(
        """
        SELECT * FROM account_video_subscriptions
        WHERE userId = :userId AND language = :language
        ORDER BY position
        """
    )
    abstract suspend fun getVideoSubscriptionEntries(
        userId: Int,
        language: String
    ): List<AccountVideoSubscriptionEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertVideoSubscriptionCache(entry: AccountVideoSubscriptionCacheEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertVideoSubscriptions(entries: List<AccountVideoSubscriptionEntry>)

    @Query("DELETE FROM account_video_subscription_caches WHERE userId = :userId")
    abstract suspend fun deleteVideoSubscriptionCaches(userId: Int)

    @Query("DELETE FROM account_video_subscriptions WHERE userId = :userId")
    abstract suspend fun deleteVideoSubscriptions(userId: Int)

    @Query(
        """
        DELETE FROM account_video_subscription_caches
        WHERE userId = :userId AND language = :language
        """
    )
    abstract suspend fun deleteVideoSubscriptionCache(userId: Int, language: String)

    @Query(
        """
        DELETE FROM account_video_subscriptions
        WHERE userId = :userId AND language = :language
        """
    )
    abstract suspend fun deleteVideoSubscriptionItems(userId: Int, language: String)

    @Transaction
    open suspend fun getVideoSubscriptions(
        userId: Int,
        language: String,
    ): AccountVideoSubscriptionsCache? {
        val entry = getVideoSubscriptionCacheEntry(userId, language) ?: return null
        return AccountVideoSubscriptionsCache(entry, getVideoSubscriptionEntries(userId, language))
    }

    @Transaction
    open suspend fun replaceVideoSubscriptions(cache: AccountVideoSubscriptionsCache) {
        val entry = cache.entry
        deleteVideoSubscriptionCache(entry.userId, entry.language)
        deleteVideoSubscriptionItems(entry.userId, entry.language)
        insertVideoSubscriptionCache(entry)
        if (cache.items.isNotEmpty()) insertVideoSubscriptions(cache.items)
    }

    @Transaction
    open suspend fun deleteVideoSubscriptionsForUser(userId: Int) {
        deleteVideoSubscriptionCaches(userId)
        deleteVideoSubscriptions(userId)
    }
}
