package su.afk.yummy.tv.data.videodownload.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.data.videodownload.R
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import javax.inject.Inject
import kotlin.math.roundToInt

internal class VideoDownloadNotificationService @Inject constructor(
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
            .build()
        return ForegroundInfo(
            notificationId(item.id),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
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
    }
}
