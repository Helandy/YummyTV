package su.afk.yummy.tv.feature.player.common.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** Тег логов сервиса плеера: по нему разбирают падения, менять нельзя. */
internal const val PLAYER_SERVICE_LOG_TAG = "PlayerMediaSession"

private const val REQUEST_CODE_SESSION_ACTIVITY = 40_101

/** Тап по уведомлению медиа-сессии возвращает в уже открытое приложение. */
internal fun Context.createPlayerSessionActivityIntent(): PendingIntent {
    val intent = packageManager.getLaunchIntentForPackage(packageName)
        ?: Intent(Intent.ACTION_MAIN).setPackage(packageName)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    return PendingIntent.getActivity(
        this,
        REQUEST_CODE_SESSION_ACTIVITY,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
