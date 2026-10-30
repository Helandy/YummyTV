package su.afk.yummy.tv.android.episodepush

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.R
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.home.model.LibraryEpisodePush
import javax.inject.Inject

/**
 * Пуш о новой серии. Основной тап и кнопка «Смотреть» ведут в `yummytv://continue/{id}` — сразу
 * плеер (без записи «продолжить» резолвер сам откроет детали), «Подробнее» — в детали тайтла.
 * Без id тайтла пуш просто открывает главную и кнопок не имеет.
 */
class NewEpisodePushNotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun showNewEpisode(notification: ProfileNotification, animeId: Int?) {
        show(
            channel = Channel.PROFILE,
            notificationId = NOTIFICATION_ID_BASE + notification.id,
            title = notification.title.ifBlank { notification.type },
            text = notification.text,
            animeId = animeId,
        )
    }

    /**
     * Локальный пуш о серии тайтла из списков пользователя. Id уведомления привязан к тайтлу, а
     * не к серии: новая серия заменяет предыдущую, а не копит стопку.
     */
    fun showLibraryEpisode(push: LibraryEpisodePush) {
        show(
            channel = Channel.LIBRARY,
            notificationId = LIBRARY_NOTIFICATION_ID_BASE + push.animeId,
            title = push.title,
            text = context.getString(R.string.library_episode_push_text, push.episodeNumber),
            animeId = push.animeId,
        )
    }

    private fun show(channel: Channel, notificationId: Int, title: String, text: String, animeId: Int?) {
        ensureChannel(channel)
        val builder = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_notification_app)
            .setColor(ACCENT_COLOR)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
        if (animeId != null) {
            val watchIntent = deepLinkIntent("$CONTINUE_DEEP_LINK_PREFIX$animeId")
            builder
                .setContentIntent(watchIntent)
                .addAction(
                    0,
                    context.getString(R.string.episode_push_action_watch),
                    watchIntent,
                )
                .addAction(
                    0,
                    context.getString(R.string.episode_push_action_details),
                    deepLinkIntent("$DETAILS_DEEP_LINK_PREFIX$animeId"),
                )
        } else {
            builder.setContentIntent(deepLinkIntent(HOME_DEEP_LINK))
        }
        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, builder.build())
    }

    private fun deepLinkIntent(deepLink: String): PendingIntent {
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

    /**
     * Создаёт канал или обновляет его название: повторный вызов безопасен и не трогает настройки,
     * которые пользователь уже поменял (звук, важность), но подтягивает новое имя после апдейта.
     */
    private fun ensureChannel(channel: Channel) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                channel.id,
                context.getString(channel.nameRes),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    /** Два канала, чтобы пуши профиля и локальные пуши настраивались в системе отдельно. */
    private enum class Channel(val id: String, @StringRes val nameRes: Int) {
        /** Серверные пуши по ленте уведомлений профиля; id канала остался прежним. */
        PROFILE("new_episodes", R.string.episode_push_notification_channel),

        /** Локальные пуши о новых сериях из списков пользователя. */
        LIBRARY("library_new_episodes", R.string.library_episode_push_notification_channel),
    }

    private companion object {
        const val NOTIFICATION_ID_BASE = 61_000
        const val LIBRARY_NOTIFICATION_ID_BASE = 1_000_000

        // PendingIntent различаются по data-ссылке, поэтому «Смотреть» и «Подробнее» не
        // перетирают друг друга при общем requestCode.
        const val CONTENT_REQUEST_CODE = 61_000
        const val CONTINUE_DEEP_LINK_PREFIX = "yummytv://continue/"
        const val DETAILS_DEEP_LINK_PREFIX = "yummytv://details/"
        const val HOME_DEEP_LINK = "yummytv://home"
        const val ACCENT_COLOR = 0xFFFF6666.toInt()
    }
}
