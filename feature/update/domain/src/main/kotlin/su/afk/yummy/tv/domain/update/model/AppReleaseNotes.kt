package su.afk.yummy.tv.domain.update.model

/**
 * Список изменений опубликованного релиза для истории версий. В отличие от [AppRelease] не требует
 * приложенного APK: историю показываем и для релизов, которые уже нельзя установить.
 * [publishedDate] — дата публикации в ISO-формате (`2026-09-20`), если GitHub её отдал.
 */
data class AppReleaseNotes(
    val version: String,
    val publishedDate: String?,
    val changelog: String,
    val isPrerelease: Boolean,
)
