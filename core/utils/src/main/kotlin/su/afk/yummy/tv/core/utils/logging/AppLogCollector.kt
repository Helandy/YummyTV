package su.afk.yummy.tv.core.utils.logging

import android.content.Context
import android.os.Build
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.utils.coroutines.di.IoApplicationScope
import su.afk.yummy.tv.core.utils.logging.utils.buildLogSessionHeader
import su.afk.yummy.tv.core.utils.logging.utils.isLineOfProcess
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Пишет всё, что процесс приложения отправляет в logcat, в [AppLogFileStore], а необработанные
 * исключения — синхронно, перед падением. Благодаря этому пользователь может передать дамп,
 * равный по содержимому logcat, включая прошлый запуск после краша.
 */
@Singleton
class AppLogCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    @param:IoApplicationScope private val scope: CoroutineScope,
    private val store: AppLogFileStore,
) {
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        store.append(buildLogSessionHeader(context))
        installCrashHandler()
        scope.launch { collectLogcat() }
    }

    private fun collectLogcat() {
        val pid = Process.myPid()
        val supportsPidFilter = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        val command = buildList {
            add("logcat")
            add("-v")
            add("threadtime")
            if (supportsPidFilter) add("--pid=$pid")
        }
        val process = runCatching { ProcessBuilder(command).redirectErrorStream(true).start() }
            .getOrElse {
                store.append("!!! logcat не запущен: $it")
                return
            }
        runCatching {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (supportsPidFilter || isLineOfProcess(line, pid)) store.append(line)
                }
            }
        }
        process.destroy()
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                store.append("!!! FATAL EXCEPTION in thread \"${thread.name}\"")
                store.append(throwable.stackTraceToString())
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
