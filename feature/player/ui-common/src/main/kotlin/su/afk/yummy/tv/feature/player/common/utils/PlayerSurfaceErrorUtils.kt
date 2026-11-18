package su.afk.yummy.tv.feature.player.common.utils

import androidx.media3.common.PlaybackException

private val OUTPUT_SURFACE_METHODS = setOf("setOutputSurface", "native_setSurface")

/**
 * Прошивка отвергла смену выходной поверхности уже работающего видеодекодера
 * (`MediaCodec.setOutputSurface` → `IllegalArgumentException`; на Amlogic Android 9 это бывает при смене
 * потока с переиспользованием кодека, в логе — «cannot change usage»). Тот же сценарий упадёт так же, пока
 * поток меняется на лету.
 */
internal fun PlaybackException.isOutputSurfaceChangeFailure(): Boolean {
    var cause: Throwable? = this
    while (cause != null) {
        if (cause is IllegalArgumentException && cause.stackTrace.any { it.methodName in OUTPUT_SURFACE_METHODS }) {
            return true
        }
        cause = cause.cause.takeIf { it !== cause }
    }
    return false
}
