package su.afk.yummy.tv.android.startup

import android.util.Log
import coil3.util.Logger

/**
 * Пишет в logcat ошибки и предупреждения Coil (сеть, декодирование, отсутствие декодера),
 * в том числе в release: иначе по логу пользователя не понять, почему не загрузилась картинка.
 * Отладочные сообщения Coil не выводим.
 */
class CoilErrorLogger : Logger {

    override var minLevel: Logger.Level = Logger.Level.Warn

    override fun log(tag: String, level: Logger.Level, message: String?, throwable: Throwable?) {
        if (level < minLevel) return
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
