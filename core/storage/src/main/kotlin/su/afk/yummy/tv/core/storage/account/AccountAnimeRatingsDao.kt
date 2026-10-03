package su.afk.yummy.tv.core.storage.account

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class AccountAnimeRatingsDao {

    @Query("SELECT * FROM account_rating_bucket_caches WHERE animeId = :animeId LIMIT 1")
    abstract suspend fun getRatingBucketCacheEntry(animeId: Int): AccountRatingBucketCacheEntry?

    @Query("SELECT * FROM account_rating_buckets WHERE animeId = :animeId ORDER BY position")
    abstract suspend fun getRatingBucketEntries(animeId: Int): List<AccountRatingBucketEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRatingBucketCache(entry: AccountRatingBucketCacheEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRatingBuckets(entries: List<AccountRatingBucketEntry>)

    @Query("DELETE FROM account_rating_bucket_caches WHERE animeId = :animeId")
    abstract suspend fun deleteRatingBucketCache(animeId: Int)

    @Query("DELETE FROM account_rating_buckets WHERE animeId = :animeId")
    abstract suspend fun deleteRatingBuckets(animeId: Int)

    @Query("SELECT * FROM account_user_ratings WHERE userId = :userId AND animeId = :animeId LIMIT 1")
    abstract suspend fun getUserRating(userId: Int, animeId: Int): AccountUserRatingEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserRating(entry: AccountUserRatingEntry)

    @Query("DELETE FROM account_user_ratings WHERE userId = :userId")
    abstract suspend fun deleteUserRatings(userId: Int)

    @Query("SELECT * FROM account_list_stats_caches WHERE animeId = :animeId LIMIT 1")
    abstract suspend fun getListStatsCacheEntry(animeId: Int): AccountListStatsCacheEntry?

    @Query("SELECT * FROM account_list_stats WHERE animeId = :animeId")
    abstract suspend fun getListStatsEntries(animeId: Int): List<AccountListStatEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertListStatsCache(entry: AccountListStatsCacheEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertListStats(entries: List<AccountListStatEntry>)

    @Query("DELETE FROM account_list_stats_caches WHERE animeId = :animeId")
    abstract suspend fun deleteListStatsCache(animeId: Int)

    @Query("DELETE FROM account_list_stats WHERE animeId = :animeId")
    abstract suspend fun deleteListStats(animeId: Int)

    @Transaction
    open suspend fun invalidateListStats(animeId: Int) {
        deleteListStats(animeId)
        deleteListStatsCache(animeId)
    }

    @Transaction
    open suspend fun getRatingBuckets(animeId: Int): AccountRatingBucketsCache? {
        val entry = getRatingBucketCacheEntry(animeId) ?: return null
        return AccountRatingBucketsCache(entry, getRatingBucketEntries(animeId))
    }

    @Transaction
    open suspend fun replaceRatingBuckets(cache: AccountRatingBucketsCache) {
        val animeId = cache.entry.animeId
        deleteRatingBucketCache(animeId)
        deleteRatingBuckets(animeId)
        insertRatingBucketCache(cache.entry)
        if (cache.buckets.isNotEmpty()) insertRatingBuckets(cache.buckets)
    }

    @Transaction
    open suspend fun deleteRatingBucketsCache(animeId: Int) {
        deleteRatingBucketCache(animeId)
        deleteRatingBuckets(animeId)
    }

    @Transaction
    open suspend fun getListStats(animeId: Int): AccountListStatsCache? {
        val entry = getListStatsCacheEntry(animeId) ?: return null
        return AccountListStatsCache(entry, getListStatsEntries(animeId))
    }

    @Transaction
    open suspend fun replaceListStats(cache: AccountListStatsCache) {
        val animeId = cache.entry.animeId
        deleteListStatsCache(animeId)
        deleteListStats(animeId)
        insertListStatsCache(cache.entry)
        if (cache.stats.isNotEmpty()) insertListStats(cache.stats)
    }
}
