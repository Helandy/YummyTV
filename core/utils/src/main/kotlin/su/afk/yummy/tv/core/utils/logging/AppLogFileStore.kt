package su.afk.yummy.tv.core.utils.logging

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ротируемое файловое хранилище логов приложения: `current.log` и предыдущий `previous.log`.
 * Лежит в `cacheDir/logs`, поэтому не попадает в бэкап; [clear] стирает всё при выключении записи. Запись потокобезопасна: в неё пишут и
 * читатель logcat, и обработчик необработанных исключений.
 */
@Singleton
class AppLogFileStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    val directory: File = File(context.cacheDir, LOGS_DIR)

    private val currentFile = File(directory, CURRENT_FILE)
    private val previousFile = File(directory, PREVIOUS_FILE)
    private var writer: BufferedWriter? = null
    private var currentSize = 0L

    /** Файлы логов от старых к новым; несуществующие пропускаются. */
    val files: List<File>
        get() = listOf(previousFile, currentFile).filter { it.isFile && it.length() > 0 }

    @Synchronized
    fun append(line: String) {
        runCatching {
            val out = writer ?: openWriter().also { writer = it }
            out.write(line)
            out.newLine()
            out.flush()
            currentSize += line.length + 1
            if (currentSize >= MAX_FILE_BYTES) rotate()
        }
    }

    @Synchronized
    private fun openWriter(): BufferedWriter {
        directory.mkdirs()
        currentSize = currentFile.length()
        return BufferedWriter(FileWriter(currentFile, true))
    }

    /** Стирает все файлы логов вместе с папкой экспорта: нужен при выключении записи. */
    @Synchronized
    fun clear() {
        runCatching { writer?.close() }
        writer = null
        currentSize = 0L
        runCatching { directory.deleteRecursively() }
    }

    private fun rotate() {
        runCatching { writer?.close() }
        writer = null
        previousFile.delete()
        currentFile.renameTo(previousFile)
        currentSize = 0L
    }

    private companion object {
        const val LOGS_DIR = "logs"
        const val CURRENT_FILE = "current.log"
        const val PREVIOUS_FILE = "previous.log"
        const val MAX_FILE_BYTES = 2L * 1024 * 1024
    }
}
