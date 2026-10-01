package su.afk.yummy.tv.data.update.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.data.update.R
import su.afk.yummy.tv.data.update.service.UpdateDownloadService
import javax.inject.Inject

internal class UpdateDownloadNotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun createProgress(progressPercent: Int): Notification {
        ensureChannel()
        val progress = progressPercent.coerceIn(0, PROGRESS_MAX)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.update_download_notification_title))
            .setContentText(context.getString(R.string.update_download_notification_progress, progress))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(PROGRESS_MAX, progress, progress <= 0)
            .setContentIntent(appContentIntent())
            .addAction(0, context.getString(R.string.update_download_notification_cancel), cancelIntent())
            .build()
    }

    fun notify(notification: Notification) {
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    /** Сообщает, что файл готов, только если приложение свёрнуто: на экране установка начнётся сама. */
    suspend fun showDownloaded() {
        val isInForeground = withContext(Dispatchers.Main) {
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        if (isInForeground) return

        ensureChannel()
        notify(
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(context.getString(R.string.update_download_notification_title))
                .setContentText(context.getString(R.string.update_download_notification_done))
                .setAutoCancel(true)
                .setContentIntent(appContentIntent())
                .build(),
        )
    }

    private fun cancelIntent(): PendingIntent {
        val intent = Intent(context, UpdateDownloadService::class.java)
            .setAction(UpdateDownloadService.ACTION_CANCEL)
        return PendingIntent.getService(
            context,
            CANCEL_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Тап открывает приложение: диалог обновления подключится к готовой загрузке и начнёт установку. */
    private fun appContentIntent(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
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
                context.getString(R.string.update_download_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
        )
    }

    companion object {
        const val NOTIFICATION_ID = 57_000
        private const val CHANNEL_ID = "update_download"
        private const val CONTENT_REQUEST_CODE = 57_001
        private const val CANCEL_REQUEST_CODE = 57_002
        private const val PROGRESS_MAX = 100
    }
}
