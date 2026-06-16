package su.afk.yummy.tv.core.storage.watchprogress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {

    @Query("SELECT * FROM watch_progress WHERE animeId = :animeId AND episode = :episode")
    suspend fun get(animeId: Int, episode: String): WatchProgressEntry?

    @Query("SELECT * FROM watch_progress")
    fun observeAll(): Flow<List<WatchProgressEntry>>

    @Query("SELECT * FROM watch_progress WHERE animeId = :animeId")
    fun observeByAnimeId(animeId: Int): Flow<List<WatchProgressEntry>>

    @Query(
        """
        SELECT * FROM watch_progress
        WHERE animeId > 0
            AND durationMs > 0
            AND positionMs >= :minPositionMs
            AND CAST(positionMs AS REAL) / durationMs < :maxProgress
            AND NOT EXISTS (
                SELECT 1 FROM continue_watching_suppressions AS suppressed
                WHERE suppressed.animeId = watch_progress.animeId
            )
        ORDER BY updatedAt DESC
        """
    )
    fun observeContinueWatching(
        minPositionMs: Long,
        maxProgress: Float,
    ): Flow<List<WatchProgressEntry>>

    @Query(
        """
        SELECT * FROM watch_progress
        WHERE animeId > 0
            AND durationMs > 0
            AND positionMs >= :minPositionMs
            AND CAST(positionMs AS REAL) / durationMs < :maxProgress
            AND NOT EXISTS (
                SELECT 1 FROM continue_watching_suppressions AS suppressed
                WHERE suppressed.animeId = watch_progress.animeId
            )
        ORDER BY updatedAt DESC
        """
    )
    suspend fun continueWatching(
        minPositionMs: Long,
        maxProgress: Float,
    ): List<WatchProgressEntry>

    @Query(
        """
        SELECT progress.* FROM watch_progress AS progress
        WHERE progress.videoId > 0
            AND progress.durationMs > 0
            AND progress.positionMs >= :minPositionMs
            AND NOT EXISTS (
                SELECT 1 FROM continue_watching_suppressions AS suppressed
                WHERE suppressed.animeId = progress.animeId
            )
            AND NOT EXISTS (
                SELECT 1 FROM watch_progress AS other
                WHERE other.videoId = progress.videoId
                    AND other.videoId > 0
                    AND other.durationMs > 0
                    AND other.positionMs >= :minPositionMs
                    AND (
                        other.updatedAt > progress.updatedAt
                        OR (
                            other.updatedAt = progress.updatedAt
                            AND (
                                other.animeId > progress.animeId
                                OR (
                                    other.animeId = progress.animeId
                                    AND other.episode > progress.episode
                                )
                            )
                        )
                    )
            )
        ORDER BY progress.updatedAt DESC
        LIMIT :limit
        """
    )
    suspend fun latestMeaningfulVideoProgress(
        minPositionMs: Long,
        limit: Int,
    ): List<WatchProgressEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entry: WatchProgressEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSuppression(entry: ContinueWatchingSuppressionEntry)

    @Query("SELECT animeId FROM continue_watching_suppressions")
    suspend fun suppressedAnimeIds(): List<Int>

    @Query("DELETE FROM continue_watching_suppressions WHERE animeId = :animeId")
    suspend fun deleteSuppression(animeId: Int)

    @Query("DELETE FROM watch_progress WHERE animeId = :animeId AND episode = :episode")
    suspend fun delete(animeId: Int, episode: String)

    @Query("DELETE FROM watch_progress WHERE animeId = :animeId")
    suspend fun deleteByAnimeId(animeId: Int)
}
