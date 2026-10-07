package su.afk.yummy.tv.core.utils.logging.utils

import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Заголовок начала сессии логов: версия приложения, устройство и Android. */
internal fun buildLogSessionHeader(context: Context): String {
    val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    val versionCode = info?.let { PackageInfoCompat.getLongVersionCode(it) }
    val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())
    return buildString {
        appendLine("==================== SESSION START ====================")
        appendLine("Time: $time")
        appendLine("App: ${context.packageName} ${info?.versionName} ($versionCode)")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        append("=======================================================")
    }
}

/** Для API < 24, где у logcat нет `--pid`: строка формата threadtime принадлежит процессу [pid]. */
internal fun isLineOfProcess(line: String, pid: Int): Boolean {
    // "MM-DD HH:MM:SS.mmm  PID  TID L tag: msg"
    val parts = line.trimStart().split(Regex("\\s+"), limit = 5)
    return parts.getOrNull(2)?.toIntOrNull() == pid
}

private val urlRegex = Regex("""(https?://[^/\s"'<>?#]+)[^\s"'<>]*""")
private val secretParamRegex =
    Regex("""\b(token|sig|signature|key|auth|session|hash|expires)=[^&\s"']+""", RegexOption.IGNORE_CASE)

/**
 * Оставляет от ссылок только схему и хост, а значения секретоподобных параметров заменяет на `***`:
 * в файл логов, который пользователь отправляет разработчику, не должны попадать подписанные ссылки.
 */
internal fun String.maskSensitiveUrls(): String =
    replace(urlRegex) { "${it.groupValues[1]}/..." }
        .replace(secretParamRegex) { "${it.groupValues[1]}=***" }

/** Строка в формате `logcat -v threadtime`, чтобы один `grep` работал и по logcat-, и по этим записям. */
internal fun formatDiagnosticLogLine(
    timeMillis: Long,
    pid: Int,
    tid: Int,
    tag: String,
    message: String,
    stackTrace: String?,
): String {
    val time = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Date(timeMillis))
    val body = if (stackTrace.isNullOrBlank()) message else "$message\n${stackTrace.trimEnd()}"
    return "$time $pid $tid D $tag: $body"
}
