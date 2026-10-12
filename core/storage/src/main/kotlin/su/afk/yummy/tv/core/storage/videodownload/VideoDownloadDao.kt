package su.afk.yummy.tv.core.storage.videodownload

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDownloadDao {
    @Query("SELECT * FROM video_downloads ORDER BY createdAt DESC, id DESC")
    fun observeDownloads(): Flow<List<VideoDownloadEntry>>

    @Query("SELECT * FROM video_downloads WHERE animeId = :animeId ORDER BY createdAt DESC, id DESC")
    fun observeDownloadsForAnime(animeId: Int): Flow<List<VideoDownloadEntry>>

    @Query("SELECT * FROM video_downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VideoDownloadEntry?

    @Query(
        """
        SELECT * FROM video_downloads
        WHERE status IN ('Resolving', 'Queued', 'Downloading')
        """,
    )
    suspend fun getUnfinishedDownloads(): List<VideoDownloadEntry>

    @Query(
        """
        SELECT * FROM video_downloads
        WHERE exportStatus IN ('Queued', 'Preparing', 'Copying')
        """,
    )
    suspend fun getUnfinishedExports(): List<VideoDownloadEntry>

    @Query(
        """
        SELECT * FROM video_downloads
        WHERE animeId = :animeId
          AND episode = :episode
        LIMIT 1
        """,
    )
    suspend fun findEpisodeDownload(
        animeId: Int,
        episode: String,
    ): VideoDownloadEntry?

    @Query(
        """
        SELECT * FROM video_downloads
        WHERE animeId = :animeId
          AND episode = :episode
        """,
    )
    suspend fun getEpisodeDownloads(
        animeId: Int,
        episode: String,
    ): List<VideoDownloadEntry>

    @Query(
        """
        UPDATE video_downloads
        SET status = 'Deleted', progress = 0, errorMessage = NULL, updatedAt = :updatedAt
        WHERE animeId = :animeId
          AND episode = :episode
        """,
    )
    suspend fun markEpisodeDeleted(
        animeId: Int,
        episode: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE video_downloads
        SET status = 'Deleted', progress = 0, errorMessage = NULL, updatedAt = :updatedAt
        WHERE animeId = :animeId
          AND episode = :episode
          AND status = 'Failed'
          AND id != :keepId
        """,
    )
    suspend fun markOtherFailedEpisodeDownloadsDeleted(
        animeId: Int,
        episode: String,
        keepId: Long,
        updatedAt: Long,
    )

    @Query(
        """
        SELECT DISTINCT cacheKey FROM video_downloads
        WHERE status != 'Deleted'
        """,
    )
    suspend fun getActiveCacheKeys(): List<String>

    /**
     * Есть ли ещё активные загрузки со старой схемой ключей. Пока такие есть, ресурсы в кэше,
     * лежащие под сырыми URL, невозможно приписать конкретной загрузке — чистить их нельзя.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM video_downloads
            WHERE status != 'Deleted'
              AND cacheKeyScheme = 0
        )
        """,
    )
    suspend fun hasActiveLegacyCacheKeyDownloads(): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: VideoDownloadEntry): Long

    @Update
    suspend fun update(entry: VideoDownloadEntry)

    /**
     * Атомарная запись статуса: условие «не Deleted/Paused» проверяется в самом UPDATE, а не
     * по прочитанной ранее записи, иначе поздний снимок прогресса перетирает «Paused» от пользователя.
     * Возвращает число затронутых строк (0 — запись заморожена или удалена).
     */
    @Query(
        """
        UPDATE video_downloads
        SET status = :status,
            progress = COALESCE(:progress, progress),
            bytesDownloaded = COALESCE(:bytesDownloaded, bytesDownloaded),
            totalBytes = COALESCE(:totalBytes, totalBytes),
            errorMessage = :errorMessage,
            updatedAt = :updatedAt
        WHERE id = :id AND status NOT IN ('Deleted', 'Paused')
        """,
    )
    suspend fun updateStatusUnlessFrozen(
        id: Long,
        status: String,
        progress: Float?,
        bytesDownloaded: Long?,
        totalBytes: Long?,
        errorMessage: String?,
        updatedAt: Long,
    ): Int
}
