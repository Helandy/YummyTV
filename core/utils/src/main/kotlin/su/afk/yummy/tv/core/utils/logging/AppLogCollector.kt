package su.afk.yummy.tv.core.utils.logging

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.utils.coroutines.di.IoApplicationScope
import su.afk.yummy.tv.core.utils.logging.utils.buildLogSessionHeader
import su.afk.yummy.tv.core.utils.logging.utils.isLineOfProcess
import javax.inject.Inject
import javax.inject.Singleton
import android.os.Process as AndroidProcess

/**
 * Пишет всё, что процесс приложения отправляет в logcat, в [AppLogFileStore], а необработанные
 * исключения — синхронно, перед падением. Благодаря этому пользователь может передать дамп,
 * равный по содержимому logcat, включая прошлый запуск после краша.
 *
 * Работает только пока запись включена в настройках ([AppLogRecordingSettings]): включение
 * запускает чтение logcat, выключение останавливает его и стирает файлы.
 */
@Singleton
class AppLogCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    @param:IoApplicationScope private val scope: CoroutineScope,
    private val store: AppLogFileStore,
    private val recordingSettings: AppLogRecordingSettings,
) {
    private val lock = Any()
    private var observing = false
    private var crashHandlerInstalled = false
    private var recordingJob: Job? = null
    private var logcatProcess: Process? = null

    /** Начинает следить за настройкой записи; безопасно вызывать повторно. */
    fun start() {
        synchronized(lock) {
            if (observing) return
            observing = true
        }
        scope.launch {
            recordingSettings.enabled.distinctUntilChanged().collect { enabled ->
                if (enabled) startRecording() else stopRecording()
            }
        }
    }

    private fun startRecording() {
        synchronized(lock) {
            if (recordingJob != null) return
            store.append(buildLogSessionHeader(context))
            installCrashHandler()
            recordingJob = scope.launch { collectLogcat() }
        }
    }

    private fun stopRecording() {
        synchronized(lock) {
            // destroy() закрывает поток logcat: блокирующее чтение возвращается и корутина завершается.
            logcatProcess?.destroy()
            logcatProcess = null
            recordingJob?.cancel()
            recordingJob = null
        }
        // Первое событие потока — реальное значение из хранилища, а не стартовое по умолчанию,
        // поэтому стирание безопасно: логи прошлого сеанса стираются только если запись выключена.
        store.clear()
    }

    private fun collectLogcat() {
        val pid = AndroidProcess.myPid()
        val supportsPidFilter = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        val command = buildList {
            add("logcat")
            add("-v")
            add("threadtime")
            // Только новые строки: история буфера до включения записи в файл попадать не должна.
            add("-T")
            add("1")
            if (supportsPidFilter) add("--pid=$pid")
        }
        val process = runCatching { ProcessBuilder(command).redirectErrorStream(true).start() }
            .getOrElse {
                store.append("!!! logcat не запущен: $it")
                return
            }
        synchronized(lock) {
            if (recordingJob == null) {
                // Запись выключили, пока процесс стартовал.
                process.destroy()
                return
            }
            logcatProcess = process
        }
        runCatching {
            process.inputStream.bufferedReader().useLines { lines ->
                // Проверка флага: после выключения буферизованные строки не должны воскресить файл.
                lines.takeWhile { recordingSettings.isEnabled }.forEach { line ->
                    if (supportsPidFilter || isLineOfProcess(line, pid)) store.append(line)
                }
            }
        }
        process.destroy()
    }

    private fun installCrashHandler() {
        if (crashHandlerInstalled) return
        crashHandlerInstalled = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (recordingSettings.isEnabled) {
                runCatching {
                    store.append("!!! FATAL EXCEPTION in thread \"${thread.name}\"")
                    store.append(throwable.stackTraceToString())
                }
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
