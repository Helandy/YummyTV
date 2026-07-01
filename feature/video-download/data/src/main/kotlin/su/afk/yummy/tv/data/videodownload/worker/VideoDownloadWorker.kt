package su.afk.yummy.tv.data.videodownload.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.dash.offline.DashDownloader
import androidx.media3.exoplayer.hls.offline.HlsDownloader
import androidx.media3.exoplayer.offline.Downloader
import androidx.media3.exoplayer.offline.ProgressiveDownloader
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.data.videodownload.cache.VideoDownloadCacheProvider
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@HiltWorker
class VideoDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: VideoDownloadRepository,
    private val cacheProvider: VideoDownloadCacheProvider,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_DOWNLOAD_ID, 0L).takeIf { it > 0L }
            ?: return Result.failure()
        val item = repository.getDownload(id) ?: return Result.failure()
        setForeground(createForegroundInfo(item.animeTitle, item.episode, 0))
        repository.updateStatus(
            id = id,
            status = VideoDownloadStatus.Downloading,
            progress = 0f,
            bytesDownloaded = 0L,
            totalBytes = null,
            errorMessage = null,
        )

        return try {
            withContext(Dispatchers.IO) {
                val upstream = DefaultHttpDataSource.Factory().apply {
                    item.headers.userAgent()?.takeIf { it.isNotBlank() }?.let(::setUserAgent)
                    val requestHeaders =
                        item.headers.filterKeys { !it.equals(USER_AGENT_HEADER, ignoreCase = true) }
                    if (requestHeaders.isNotEmpty()) setDefaultRequestProperties(requestHeaders)
                }
                val cacheDataSource = CacheDataSource.Factory()
                    .setCache(cacheProvider.cache)
                    .setUpstreamDataSourceFactory(upstream)
                val mediaItem = MediaItem.Builder()
                    .setUri(item.streamUrl)
                    .setCustomCacheKey(item.cacheKey)
                    .build()
                var lastProgress = -1
                val downloader = createDownloader(mediaItem, cacheDataSource)
                downloader.download { contentLength, bytesDownloaded, percentDownloaded ->
                    val total = contentLength.takeIf { it > 0L }
                    val progress = if (percentDownloaded >= 0f) {
                        (percentDownloaded / 100f).coerceIn(0f, 1f)
                    } else if (total != null) {
                        (bytesDownloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    val progressPercent = (progress * 100).roundToInt()
                    if (progressPercent != lastProgress) {
                        lastProgress = progressPercent
                        setProgressAsync(
                            androidx.work.workDataOf(
                                KEY_PROGRESS to progressPercent,
                            )
                        )
                        runBlocking {
                            repository.updateStatus(
                                id = id,
                                status = VideoDownloadStatus.Downloading,
                                progress = progress,
                                bytesDownloaded = bytesDownloaded,
                                totalBytes = total,
                                errorMessage = null,
                            )
                        }
                    }
                }
            }
            repository.updateStatus(
                id = id,
                status = VideoDownloadStatus.Downloaded,
                progress = 1f,
                errorMessage = null,
            )
            Result.success()
        } catch (throwable: Throwable) {
            repository.updateStatus(
                id = id,
                status = VideoDownloadStatus.Failed,
                errorMessage = throwable.localizedMessage ?: throwable.message.orEmpty(),
            )
            Result.failure()
        }
    }

    private fun createForegroundInfo(
        title: String,
        episode: String,
        progress: Int
    ): ForegroundInfo {
        ensureChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title.ifBlank { "YummyTV" })
            .setContentText("Episode $episode")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progress, progress <= 0)
            .build()
        return ForegroundInfo(
            NOTIFICATION_ID_BASE + id.hashCode(),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun ensureChannel() {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Video downloads",
                NotificationManager.IMPORTANCE_LOW,
            )
        )
    }

    private fun createDownloader(
        mediaItem: MediaItem,
        cacheDataSourceFactory: CacheDataSource.Factory,
    ): Downloader {
        val cleanUrl = mediaItem.localConfiguration?.uri?.toString()
            ?.substringBefore('?')
            ?.substringBefore('#')
            .orEmpty()
        return when {
            cleanUrl.endsWith(".m3u8", ignoreCase = true) ->
                HlsDownloader.Factory(cacheDataSourceFactory).create(mediaItem)

            cleanUrl.endsWith(".mpd", ignoreCase = true) ->
                DashDownloader.Factory(cacheDataSourceFactory).create(mediaItem)

            else -> ProgressiveDownloader(mediaItem, cacheDataSourceFactory)
        }
    }

    private fun Map<String, String>.userAgent(): String? =
        entries.firstOrNull { (key, _) -> key.equals(USER_AGENT_HEADER, ignoreCase = true) }?.value

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_PROGRESS = "progress"
        private const val CHANNEL_ID = "video_downloads"
        private const val NOTIFICATION_ID_BASE = 56_000
        private const val USER_AGENT_HEADER = "User-Agent"
    }
}
