package su.afk.yummy.tv.android.startup.utils

import android.app.ActivityManager
import android.app.ApplicationStartInfo
import android.content.Intent
import android.os.Build
import su.afk.yummy.tv.android.startup.model.StartupEntry
import kotlin.math.ceil

/**
 * Корзина длительности для консоли AppMetrica: строковые значения там группируются, а сырые
 * миллисекунды полезны только в выгрузке.
 */
fun Long.toStartupDurationBucket(): String = when {
    this < 500 -> "<500"
    this < 1_000 -> "500-1000"
    this < 2_000 -> "1000-2000"
    this < 4_000 -> "2000-4000"
    else -> "4000+"
}

/** Объём RAM устройства в гигабайтах с округлением вверх: 1.8 ГБ приставки → 2. */
fun ActivityManager.totalRamGb(): Int {
    val info = ActivityManager.MemoryInfo()
    getMemoryInfo(info)
    return ceil(info.totalMem.toDouble() / BYTES_IN_GB).toInt()
}

/** Процесс поднят ради видимой Activity, а не WorkManager/пуша/поискового провайдера. */
fun isProcessStartedInForeground(): Boolean {
    val state = ActivityManager.RunningAppProcessInfo()
    ActivityManager.getMyMemoryState(state)
    return state.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
}

fun Intent.toStartupEntry(): StartupEntry = when {
    data != null -> StartupEntry.DEEPLINK
    action == Intent.ACTION_MAIN && (
        hasCategory(Intent.CATEGORY_LAUNCHER) || hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        ) -> StartupEntry.LAUNCHER

    else -> StartupEntry.OTHER
}

/** Системная запись о последнем старте процесса (API 35+) — подтверждает, что старт холодный. */
fun ActivityManager.lastProcessStartInfo(): ApplicationStartInfo? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        runCatching { getHistoricalProcessStartReasons(1).firstOrNull() }.getOrNull()
    } else {
        null
    }

fun ApplicationStartInfo?.startTypeName(): String? {
    if (this == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return null
    return when (startType) {
        ApplicationStartInfo.START_TYPE_COLD -> "cold"
        ApplicationStartInfo.START_TYPE_WARM -> "warm"
        ApplicationStartInfo.START_TYPE_HOT -> "hot"
        else -> "unset"
    }
}

fun ApplicationStartInfo?.startReasonName(): String? {
    if (this == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return null
    return when (reason) {
        ApplicationStartInfo.START_REASON_LAUNCHER -> "launcher"
        ApplicationStartInfo.START_REASON_LAUNCHER_RECENTS -> "recents"
        ApplicationStartInfo.START_REASON_START_ACTIVITY -> "start_activity"
        ApplicationStartInfo.START_REASON_PUSH -> "push"
        ApplicationStartInfo.START_REASON_JOB -> "job"
        ApplicationStartInfo.START_REASON_BROADCAST -> "broadcast"
        ApplicationStartInfo.START_REASON_SERVICE -> "service"
        ApplicationStartInfo.START_REASON_CONTENT_PROVIDER -> "content_provider"
        ApplicationStartInfo.START_REASON_ALARM -> "alarm"
        ApplicationStartInfo.START_REASON_BOOT_COMPLETE -> "boot"
        else -> "other"
    }
}

private const val BYTES_IN_GB = 1024.0 * 1024 * 1024
