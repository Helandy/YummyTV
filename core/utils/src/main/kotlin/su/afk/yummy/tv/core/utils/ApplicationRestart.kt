package su.afk.yummy.tv.core.utils

import android.content.Context
import android.content.Intent

fun Context.restartApplication(): Boolean {
    val launchIntent = runCatching {
        packageManager.getLaunchIntentForPackage(packageName)
    }.getOrNull() ?: return false
    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    return runCatching { startActivity(launchIntent) }.isSuccess
}
