package su.afk.yummy.tv.core.storage.videodownload

import kotlinx.coroutines.flow.Flow

internal class VideoDownloadStore(private val dao: VideoDownloadDao) : VideoDownloadStorage {

    override fun observeDownloads(): Flow<List<VideoDownloadEntry>> = dao.observeDownloads()

    override fun observeDownloadsForAnime(animeId: Int): Flow<List<VideoDownloadEntry>> =
        dao.observeDownloadsForAnime(animeId)

    override suspend fun getById(id: Long): VideoDownloadEntry? = dao.getById(id)

    override suspend fun getUnfinishedDownloads(): List<VideoDownloadEntry> =
        dao.getUnfinishedDownloads()

    override suspend fun getUnfinishedExports(): List<VideoDownloadEntry> =
        dao.getUnfinishedExports()

    override suspend fun findEpisodeDownload(animeId: Int, episode: String): VideoDownloadEntry? =
        dao.findEpisodeDownload(animeId, episode)

    override suspend fun getEpisodeDownloads(
        animeId: Int,
        episode: String
    ): List<VideoDownloadEntry> =
        dao.getEpisodeDownloads(animeId, episode)

    override suspend fun markEpisodeDeleted(animeId: Int, episode: String, updatedAt: Long) =
        dao.markEpisodeDeleted(animeId, episode, updatedAt)

    override suspend fun markOtherFailedEpisodeDownloadsDeleted(
        animeId: Int,
        episode: String,
        keepId: Long,
        updatedAt: Long,
    ) = dao.markOtherFailedEpisodeDownloadsDeleted(animeId, episode, keepId, updatedAt)

    override suspend fun getActiveCacheKeys(): List<String> = dao.getActiveCacheKeys()

    override suspend fun hasActiveLegacyCacheKeyDownloads(): Boolean =
        dao.hasActiveLegacyCacheKeyDownloads()

    override suspend fun insert(entry: VideoDownloadEntry): Long = dao.insert(entry)

    override suspend fun update(entry: VideoDownloadEntry) = dao.update(entry)

    override suspend fun updateStatusUnlessFrozen(
        id: Long,
        status: String,
        progress: Float?,
        bytesDownloaded: Long?,
        totalBytes: Long?,
        errorMessage: String?,
        updatedAt: Long,
    ): Boolean = dao.updateStatusUnlessFrozen(
        id, status, progress, bytesDownloaded, totalBytes, errorMessage, updatedAt,
    ) > 0
}
