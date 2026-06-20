package su.afk.yummy.tv.core.logger

import android.util.Log

object AppLogger {
    fun d(tag: String, message: () -> String) {
        log(Log.DEBUG, tag, null, message)
    }

    fun d(tag: String, throwable: Throwable, message: () -> String) {
        log(Log.DEBUG, tag, throwable, message)
    }

    fun i(tag: String, message: () -> String) {
        log(Log.INFO, tag, null, message)
    }

    fun w(tag: String, throwable: Throwable? = null, message: () -> String) {
        log(Log.WARN, tag, throwable, message)
    }

    fun e(tag: String, throwable: Throwable? = null, message: () -> String) {
        log(Log.ERROR, tag, throwable, message)
    }

    private fun log(
        priority: Int,
        tag: String,
        throwable: Throwable?,
        message: () -> String,
    ) {
        if (!BuildConfig.DEBUG) return
        if (throwable == null) {
            Log.println(priority, tag, message())
        } else {
            Log.println(priority, tag, "${message()}\n${Log.getStackTraceString(throwable)}")
        }
    }
}
