package su.afk.yummy.tv.core.storage.videodownload

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDownloadDao {
    @Query("SELECT * FROM video_downloads ORDER BY updatedAt DESC")
    fun observeDownloads(): Flow<List<VideoDownloadEntry>>

    @Query("SELECT * FROM video_downloads WHERE animeId = :animeId ORDER BY updatedAt DESC")
    fun observeDownloadsForAnime(animeId: Int): Flow<List<VideoDownloadEntry>>

    @Query("SELECT * FROM video_downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VideoDownloadEntry?

    @Query(
        """
        SELECT * FROM video_downloads
        WHERE status IN ('Resolving', 'Queued', 'Downloading')
        """
    )
    suspend fun getUnfinishedDownloads(): List<VideoDownloadEntry>

    @Query(
        """
        SELECT * FROM video_downloads
        WHERE exportStatus IN ('Queued', 'Preparing', 'Copying')
        """
    )
    suspend fun getUnfinishedExports(): List<VideoDownloadEntry>

    @Query(
        """
        SELECT * FROM video_downloads
        WHERE animeId = :animeId
          AND episode = :episode
        LIMIT 1
        """
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
        """
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
        """
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
        """
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
        """
    )
    suspend fun getActiveCacheKeys(): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: VideoDownloadEntry): Long

    @Update
    suspend fun update(entry: VideoDownloadEntry)
}
