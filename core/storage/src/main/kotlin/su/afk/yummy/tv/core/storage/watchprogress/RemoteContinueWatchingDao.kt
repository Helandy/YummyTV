package su.afk.yummy.tv.core.storage.watchprogress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteContinueWatchingDao {

    @Query(
        """
        SELECT * FROM remote_continue_watching
        WHERE accountKey = :accountKey
            AND language = :language
            AND NOT EXISTS (
                SELECT 1 FROM continue_watching_suppressions AS suppressed
                WHERE suppressed.animeId = remote_continue_watching.animeId
                    AND suppressed.suppressedAt >= remote_continue_watching.updatedAt
            )
        ORDER BY updatedAt DESC
        """
    )
    fun observe(
        accountKey: String,
        language: String,
    ): Flow<List<RemoteContinueWatchingEntry>>

    @Query(
        """
        SELECT * FROM remote_continue_watching
        WHERE accountKey = :accountKey
            AND language = :language
            AND NOT EXISTS (
                SELECT 1 FROM continue_watching_suppressions AS suppressed
                WHERE suppressed.animeId = remote_continue_watching.animeId
                    AND suppressed.suppressedAt >= remote_continue_watching.updatedAt
            )
        ORDER BY updatedAt DESC
        """
    )
    suspend fun getAll(
        accountKey: String,
        language: String,
    ): List<RemoteContinueWatchingEntry>

    @Query(
        """
        SELECT * FROM remote_continue_watching
        WHERE accountKey = :accountKey
            AND language = :language
            AND animeId = :animeId
            AND targetKey = :targetKey
        LIMIT 1
        """
    )
    suspend fun get(
        accountKey: String,
        language: String,
        animeId: Int,
        targetKey: String,
    ): RemoteContinueWatchingEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entry: RemoteContinueWatchingEntry)

    @Query("DELETE FROM remote_continue_watching WHERE animeId = :animeId")
    suspend fun deleteByAnimeId(animeId: Int)

    @Query(
        """
        DELETE FROM remote_continue_watching
        WHERE accountKey = :accountKey
            AND language = :language
            AND rowid NOT IN (
                SELECT rowid
                FROM remote_continue_watching
                WHERE accountKey = :accountKey AND language = :language
                ORDER BY updatedAt DESC, cachedAt DESC
                LIMIT :limit
            )
        """
    )
    suspend fun prune(
        accountKey: String,
        language: String,
        limit: Int,
    )
}
