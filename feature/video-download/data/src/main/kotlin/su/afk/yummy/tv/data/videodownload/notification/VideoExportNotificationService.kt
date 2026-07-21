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

class VideoExportNotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun createForegroundInfo(item: VideoDownloadItem, progressPercent: Int): ForegroundInfo {
        ensureChannel()
        val progress = progressPercent.coerceIn(0, 100)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(
                context.getString(R.string.video_export_notification_title, item.episode)
            )
            .setContentText(
                context.getString(R.string.video_export_notification_progress, progress)
            )
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(PROGRESS_MAX, progress, progress == 0)
            .setContentIntent(downloadsContentIntent())
            .build()
        return ForegroundInfo(
            notificationId(item.id),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
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

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.video_export_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
        )
    }

    private fun notificationId(downloadId: Long): Int =
        NOTIFICATION_ID_BASE + downloadId.hashCode()

    private companion object {
        private const val CHANNEL_ID = "video_exports"
        private const val NOTIFICATION_ID_BASE = 57_000
        private const val PROGRESS_MAX = 100
        private const val CONTENT_REQUEST_CODE = 57_000
        private const val DOWNLOADS_DEEP_LINK = "yummytv://downloads"
    }
}
