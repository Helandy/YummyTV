package su.afk.yummy.tv.android.episodepush

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.R
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import javax.inject.Inject

class NewEpisodePushNotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun showNewEpisode(notification: ProfileNotification, animeId: Int?) {
        ensureChannel()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_app)
            .setColor(ACCENT_COLOR)
            .setContentTitle(notification.title.ifBlank { notification.type })
            .setContentText(notification.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.text))
            .setAutoCancel(true)
            .setContentIntent(contentIntent(animeId))
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID_BASE + notification.id, builder)
    }

    private fun contentIntent(animeId: Int?): PendingIntent {
        val deepLink = if (animeId != null) "$DETAILS_DEEP_LINK_PREFIX$animeId" else HOME_DEEP_LINK
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
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
                context.getString(R.string.episode_push_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    private companion object {
        const val CHANNEL_ID = "new_episodes"
        const val NOTIFICATION_ID_BASE = 61_000
        const val CONTENT_REQUEST_CODE = 61_000
        const val DETAILS_DEEP_LINK_PREFIX = "yummytv://details/"
        const val HOME_DEEP_LINK = "yummytv://home"
        const val ACCENT_COLOR = 0xFFFF6666.toInt()
    }
}
