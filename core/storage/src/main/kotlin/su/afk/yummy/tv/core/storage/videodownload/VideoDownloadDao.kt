package su.afk.yummy.tv.core.storage.videodownload

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDownloadDao {
    @Query("SELECT * FROM video_downloads WHERE status != 'Deleted' ORDER BY updatedAt DESC")
    fun observeDownloads(): Flow<List<VideoDownloadEntry>>

    @Query("SELECT * FROM video_downloads WHERE animeId = :animeId AND status != 'Deleted'")
    fun observeDownloadsForAnime(animeId: Int): Flow<List<VideoDownloadEntry>>

    @Query("SELECT * FROM video_downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VideoDownloadEntry?

    @Query(
        """
        SELECT * FROM video_downloads
        WHERE animeId = :animeId
          AND videoId = :videoId
          AND iframeUrl = :iframeUrl
          AND qualityLabel = :qualityLabel
          AND status IN ('Queued', 'Downloading', 'Downloaded', 'Resolving')
        LIMIT 1
        """
    )
    suspend fun findActiveDuplicate(
        animeId: Int,
        videoId: Int,
        iframeUrl: String,
        qualityLabel: String,
    ): VideoDownloadEntry?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: VideoDownloadEntry): Long

    @Update
    suspend fun update(entry: VideoDownloadEntry)
}
