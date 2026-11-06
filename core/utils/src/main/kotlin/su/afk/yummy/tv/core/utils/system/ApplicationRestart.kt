package su.afk.yummy.tv.core.utils.system

import android.content.Context
import android.content.Intent
import android.os.Process

fun Context.restartApplication(): Boolean {
    val launchIntent = runCatching {
        packageManager.getLaunchIntentForPackage(packageName)
    }.getOrNull() ?: return false
    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    return runCatching { startActivity(launchIntent) }.isSuccess
}

/**
 * Холодный перезапуск: стартует launch-активити в новой задаче и убивает текущий процесс,
 * чтобы всё, что читается при старте приложения, прочиталось заново.
 * Возвращает false, если запустить приложение заново не удалось (процесс при этом не тронут).
 */
fun Context.restartApplicationCold(): Boolean {
    if (!restartApplication()) return false
    Process.killProcess(Process.myPid())
    return true
}
