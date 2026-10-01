package su.afk.yummy.tv.core.utils.logging

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Собирает дамп логов в один файл и отдаёт его для передачи: Share-ссылкой или копией в память. */
@Singleton
class AppLogExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: AppLogFileStore,
) {
    /** Склеивает файлы ротации в один `.txt`; прошлые экспорты удаляются. */
    suspend fun buildDump(): File = withContext(Dispatchers.IO) {
        val exportDir = File(store.directory, EXPORT_DIR).apply {
            deleteRecursively()
            mkdirs()
        }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        File(exportDir, "yummytv-logs-$stamp.txt").also { dump ->
            dump.outputStream().use { out ->
                store.files.forEach { file -> file.inputStream().use { it.copyTo(out) } }
            }
        }
    }

    /** content:// URI для ACTION_SEND; нужен FileProvider с authority `<applicationId>.logs`. */
    fun shareUri(dump: File) = FileProvider.getUriForFile(context, "${context.packageName}.logs", dump)

    /**
     * Копирует дамп туда, где его найдёт пользователь без файлового менеджера с доступом к
     * cache: `Downloads` (API 29+) или внешняя папка приложения. Возвращает читаемый путь.
     */
    suspend fun saveToStorage(dump: File): String = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, dump.name)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
            resolver.openOutputStream(uri)!!.use { out -> dump.inputStream().use { it.copyTo(out) } }
            "${Environment.DIRECTORY_DOWNLOADS}/${dump.name}"
        } else {
            val dir = checkNotNull(context.getExternalFilesDir(null))
            File(dir, dump.name).also { dump.copyTo(it, overwrite = true) }.absolutePath
        }
    }

    private companion object {
        const val EXPORT_DIR = "export"
    }
}
