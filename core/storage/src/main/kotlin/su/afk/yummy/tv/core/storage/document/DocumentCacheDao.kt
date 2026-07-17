package su.afk.yummy.tv.core.storage.document

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DocumentCacheDao {
    @Query("SELECT * FROM document_cache WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun get(cacheKey: String): DocumentCacheEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entry: DocumentCacheEntry)

    @Query("DELETE FROM document_cache WHERE cacheKey = :cacheKey")
    suspend fun delete(cacheKey: String)

    @Query("DELETE FROM document_cache WHERE cacheKey LIKE :prefix || '%'")
    suspend fun deleteByPrefix(prefix: String)

    @Query("DELETE FROM document_cache WHERE cacheKey LIKE 'user:%:' || :namespace || ':%'")
    suspend fun deleteUserNamespace(namespace: String)
}
