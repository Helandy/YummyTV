package su.afk.yummy.tv.core.analytics.api

/**
 * Приёмник диагностических строк, которые должны пережить release-сборку: в ней logcat-трекер
 * не подключён, а файл логов приложения читает только logcat процесса.
 *
 * Реализация живёт там, где есть файловое хранилище логов; сюда попадают только теги из
 * [PersistedLogTags.persisted].
 */
interface DiagnosticLogSink {
    /** Сохраняет одну диагностическую запись; [throwable], если есть, дописывается стеком. */
    fun write(tag: String, throwable: Throwable?, message: String)
}
