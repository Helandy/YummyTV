package su.afk.yummy.tv.data.videodownload.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.data.videodownload.R
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import javax.inject.Inject
import kotlin.math.roundToInt

class VideoDownloadNotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun createForegroundInfo(item: VideoDownloadItem): ForegroundInfo =
        createForegroundInfo(
            item = item,
            progressPercent = item.progress.toPercent(),
        )

    fun createForegroundInfo(
        item: VideoDownloadItem,
        progressPercent: Int,
    ): ForegroundInfo {
        ensureChannel()
        val progress = progressPercent.coerceIn(0, 100)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(item.animeTitle.ifBlank { context.getString(R.string.video_download_notification_title) })
            .setContentText(
                context.getString(
                    R.string.video_download_notification_progress,
                    item.episode,
                    progress,
                )
            )
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(PROGRESS_MAX, progress, progress <= 0)
            .setContentIntent(downloadsContentIntent())
            .addAction(
                0,
                context.getString(R.string.video_download_notification_pause),
                commandIntent(item.id, VideoDownloadNotificationReceiver.ACTION_PAUSE),
            )
            .build()
        return ForegroundInfo(
            notificationId(item.id),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    fun showPaused(item: VideoDownloadItem) {
        ensureChannel()
        val progress = item.progress.toPercent()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(
                item.animeTitle.ifBlank {
                    context.getString(R.string.video_download_notification_title)
                }
            )
            .setContentText(
                context.getString(
                    R.string.video_download_notification_paused,
                    item.episode,
                    progress,
                )
            )
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(PROGRESS_MAX, progress, false)
            .setContentIntent(downloadsContentIntent())
            .addAction(
                0,
                context.getString(R.string.video_download_notification_resume),
                commandIntent(item.id, VideoDownloadNotificationReceiver.ACTION_RESUME),
            )
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId(item.id), notification)
    }

    fun cancel(downloadId: Long) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(notificationId(downloadId))
    }

    private fun downloadsContentIntent(): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOADS_DEEP_LINK)).apply {
            setPackage(context.packageName)
        }
        return PendingIntent.getActivity(
            context,
            CONTENT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun commandIntent(downloadId: Long, action: String): PendingIntent {
        val intent = Intent(context, VideoDownloadNotificationReceiver::class.java).apply {
            this.action = action
            putExtra(VideoDownloadNotificationReceiver.EXTRA_DOWNLOAD_ID, downloadId)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId(downloadId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.video_download_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
        )
    }

    private fun notificationId(downloadId: Long): Int =
        NOTIFICATION_ID_BASE + downloadId.hashCode()

    private fun Float.toPercent(): Int =
        (coerceIn(0f, 1f) * PROGRESS_MAX).roundToInt()

    private companion object {
        private const val CHANNEL_ID = "video_downloads"
        private const val NOTIFICATION_ID_BASE = 56_000
        private const val PROGRESS_MAX = 100
        private const val CONTENT_REQUEST_CODE = 56_000
        private const val DOWNLOADS_DEEP_LINK = "yummytv://downloads"
    }
}
