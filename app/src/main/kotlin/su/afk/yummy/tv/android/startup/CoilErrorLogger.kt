package su.afk.yummy.tv.android.startup

import android.util.Log
import coil3.util.Logger
import su.afk.yummy.tv.BuildConfig
import su.afk.yummy.tv.core.utils.logging.AppLogRecordingSettings

/**
 * Пишет в logcat ошибки и предупреждения Coil (сеть, декодирование, отсутствие декодера).
 * В release — только пока включена «Запись логов» в настройках ([AppLogRecordingSettings]),
 * в debug — всегда. Отладочные сообщения Coil не выводим.
 */
class CoilErrorLogger(
    private val logRecordingSettings: AppLogRecordingSettings,
) : Logger {

    override var minLevel: Logger.Level = Logger.Level.Warn

    override fun log(tag: String, level: Logger.Level, message: String?, throwable: Throwable?) {
        if (level < minLevel) return
        if (!BuildConfig.DEBUG && !logRecordingSettings.isEnabled) return
        val text = message.orEmpty()
        when (level) {
            Logger.Level.Error -> Log.e(LOG_TAG, "[$tag] $text", throwable)
            else -> Log.w(LOG_TAG, "[$tag] $text", throwable)
        }
    }

    private companion object {
        const val LOG_TAG = "Coil"
    }
}
