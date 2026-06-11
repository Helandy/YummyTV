package su.afk.yummy.tv.core.storage.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CacheDao {

    @Query("SELECT * FROM cache WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): CacheEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: CacheEntry)

    @Query("DELETE FROM cache WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM cache WHERE `key` LIKE :prefix || '%'")
    suspend fun deleteByPrefix(prefix: String)

    @Query("DELETE FROM cache WHERE cachedAt < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)
}
