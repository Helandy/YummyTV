package su.afk.yummy.tv.domain.update.model

/**
 * Опубликованный релиз приложения: версия без префикса тега (`v`/`b`), список изменений
 * и прямая ссылка на APK. У pre-release версия с суффиксом `-beta`, как у `versionName` беты (`1.21.1.2-beta`).
 */
data class AppRelease(
    val version: String,
    val changelog: String,
    val apkUrl: String,
    val isPrerelease: Boolean = false,
    val updatesCount: Int = 1,
)
