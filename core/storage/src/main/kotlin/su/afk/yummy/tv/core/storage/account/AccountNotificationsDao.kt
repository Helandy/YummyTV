package su.afk.yummy.tv.core.storage.account

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class AccountNotificationsDao {

    @Query(
        """
        SELECT * FROM account_notification_pages
        WHERE userId = :userId AND language = :language AND `limit` = :limit AND `offset` = :offset
        LIMIT 1
        """
    )
    abstract suspend fun getNotificationPageEntry(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): AccountNotificationPageEntry?

    @Query(
        """
        SELECT * FROM account_notifications
        WHERE userId = :userId AND language = :language AND `limit` = :limit AND `offset` = :offset
        ORDER BY position
        """
    )
    abstract suspend fun getNotificationEntries(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): List<AccountNotificationEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertNotificationPage(entry: AccountNotificationPageEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertNotifications(entries: List<AccountNotificationEntry>)

    @Query("DELETE FROM account_notification_pages WHERE userId = :userId")
    abstract suspend fun deleteNotificationPages(userId: Int)

    @Query("DELETE FROM account_notifications WHERE userId = :userId")
    abstract suspend fun deleteNotifications(userId: Int)

    @Query(
        """
        DELETE FROM account_notification_pages
        WHERE userId = :userId AND language = :language AND `limit` = :limit AND `offset` = :offset
        """
    )
    abstract suspend fun deleteNotificationPage(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    )

    @Query(
        """
        DELETE FROM account_notifications
        WHERE userId = :userId AND language = :language AND `limit` = :limit AND `offset` = :offset
        """
    )
    abstract suspend fun deleteNotificationItems(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    )

    @Query(
        """
        DELETE FROM account_notifications
        WHERE EXISTS (
            SELECT 1 FROM account_notification_pages AS page
            WHERE page.userId = account_notifications.userId
                AND page.language = account_notifications.language
                AND page.`limit` = account_notifications.`limit`
                AND page.`offset` = account_notifications.`offset`
                AND page.cachedAt < :minCachedAt
        )
        """
    )
    abstract suspend fun deleteNotificationItemsCachedBefore(minCachedAt: Long)

    @Query("DELETE FROM account_notification_pages WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteNotificationPagesCachedBefore(minCachedAt: Long)

    @Query("SELECT * FROM account_notification_count_caches WHERE userId = :userId LIMIT 1")
    abstract suspend fun getNotificationCountCacheEntry(userId: Int): AccountNotificationCountCacheEntry?

    @Query("SELECT * FROM account_notification_counts WHERE userId = :userId ORDER BY position")
    abstract suspend fun getNotificationCountEntries(userId: Int): List<AccountNotificationCountEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertNotificationCountCache(entry: AccountNotificationCountCacheEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertNotificationCounts(entries: List<AccountNotificationCountEntry>)

    @Query("DELETE FROM account_notification_count_caches WHERE userId = :userId")
    abstract suspend fun deleteNotificationCountCache(userId: Int)

    @Query("DELETE FROM account_notification_counts WHERE userId = :userId")
    abstract suspend fun deleteNotificationCounts(userId: Int)

    @Query("SELECT * FROM account_notification_anime WHERE slug = :slug LIMIT 1")
    abstract suspend fun getNotificationAnime(slug: String): AccountNotificationAnimeEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertNotificationAnime(entry: AccountNotificationAnimeEntry)

    @Transaction
    open suspend fun getNotifications(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): AccountNotificationsPageCache? {
        val entry = getNotificationPageEntry(userId, language, limit, offset) ?: return null
        return AccountNotificationsPageCache(
            entry,
            getNotificationEntries(userId, language, limit, offset)
        )
    }

    @Transaction
    open suspend fun replaceNotifications(
        cache: AccountNotificationsPageCache,
        prunePagesCachedBefore: Long? = null,
    ) {
        val entry = cache.entry
        deleteNotificationPage(entry.userId, entry.language, entry.limit, entry.offset)
        deleteNotificationItems(entry.userId, entry.language, entry.limit, entry.offset)
        insertNotificationPage(entry)
        if (cache.items.isNotEmpty()) insertNotifications(cache.items)

        prunePagesCachedBefore?.let {
            deleteNotificationItemsCachedBefore(it)
            deleteNotificationPagesCachedBefore(it)
        }
    }

    @Transaction
    open suspend fun deleteNotificationsForUser(userId: Int) {
        deleteNotificationPages(userId)
        deleteNotifications(userId)
    }

    @Transaction
    open suspend fun getNotificationCounts(userId: Int): AccountNotificationCountsCache? {
        val entry = getNotificationCountCacheEntry(userId) ?: return null
        return AccountNotificationCountsCache(entry, getNotificationCountEntries(userId))
    }

    @Transaction
    open suspend fun replaceNotificationCounts(cache: AccountNotificationCountsCache) {
        val userId = cache.entry.userId
        deleteNotificationCountCache(userId)
        deleteNotificationCounts(userId)
        insertNotificationCountCache(cache.entry)
        if (cache.items.isNotEmpty()) insertNotificationCounts(cache.items)
    }
}
