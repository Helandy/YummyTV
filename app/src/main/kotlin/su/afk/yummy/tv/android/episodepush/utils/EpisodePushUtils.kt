package su.afk.yummy.tv.android.episodepush.utils

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

const val EXTRA_EPISODE_PUSH_NOTIFICATION_ID = "su.afk.yummy.tv.extra.EPISODE_PUSH_NOTIFICATION_ID"

/**
 * Закрывает пуш о новой серии, из которого открыт intent. `setAutoCancel` убирает уведомление
 * только по тапу на само тело, кнопки «Смотреть»/«Подробнее» его не закрывают. Extra
 * удаляется, чтобы повторная обработка того же intent не отменяла чужое уведомление.
 */
fun Intent.cancelEpisodePushNotification(context: Context) {
    val id = getIntExtra(EXTRA_EPISODE_PUSH_NOTIFICATION_ID, NO_NOTIFICATION_ID)
    if (id == NO_NOTIFICATION_ID) return
    removeExtra(EXTRA_EPISODE_PUSH_NOTIFICATION_ID)
    NotificationManagerCompat.from(context).cancel(id)
}

private const val NO_NOTIFICATION_ID = -1
