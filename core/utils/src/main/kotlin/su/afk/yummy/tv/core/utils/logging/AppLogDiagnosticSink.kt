package su.afk.yummy.tv.core.utils.logging

import android.os.Process
import android.util.Log
import su.afk.yummy.tv.core.analytics.api.DiagnosticLogSink
import su.afk.yummy.tv.core.utils.logging.utils.formatDiagnosticLogLine
import su.afk.yummy.tv.core.utils.logging.utils.maskSensitiveUrls
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Пишет диагностику плеера прямо в [AppLogFileStore], минуя logcat: в release logcat-трекера нет,
 * а [AppLogCollector] читает только logcat. В debug не используется, иначе строки продублировались
 * бы: там их уже подбирает коллектор.
 *
 * Запись синхронная: строк мало (единицы в минуту), хранилище и так сбрасывает буфер на каждой
 * строке, зато хвост не теряется при падении. Ссылки в тексте усекаются до хоста. Пока запись
 * логов выключена в настройках ([AppLogRecordingSettings]), строки отбрасываются.
 */
@Singleton
class AppLogDiagnosticSink @Inject constructor(
    private val store: AppLogFileStore,
    private val recordingSettings: AppLogRecordingSettings,
) : DiagnosticLogSink {

    override fun write(tag: String, throwable: Throwable?, message: String) {
        if (!recordingSettings.isEnabled) return
        store.append(
            formatDiagnosticLogLine(
                timeMillis = System.currentTimeMillis(),
                pid = Process.myPid(),
                tid = Process.myTid(),
                tag = tag,
                message = message.maskSensitiveUrls(),
                stackTrace = throwable?.let { Log.getStackTraceString(it).maskSensitiveUrls() },
            ),
        )
    }
}
