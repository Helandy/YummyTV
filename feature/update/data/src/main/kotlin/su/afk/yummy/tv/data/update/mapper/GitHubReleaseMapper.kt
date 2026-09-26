package su.afk.yummy.tv.data.update.mapper

import su.afk.yummy.tv.data.update.dto.GitHubReleaseDto
import su.afk.yummy.tv.domain.update.model.AppRelease
import su.afk.yummy.tv.domain.update.model.AppReleaseNotes
import su.afk.yummy.tv.domain.update.util.PRERELEASE_VERSION_SUFFIX

private const val STABLE_TAG_PREFIX = 'v'
private const val BETA_TAG_PREFIX = 'b'

/** Длина даты `yyyy-MM-dd` в начале ISO-времени `published_at` (`2026-09-20T12:00:00Z`). */
private const val ISO_DATE_LENGTH = 10

/**
 * Релиз без приложенного APK установить нельзя, поэтому такой релиз (как и черновик) маппится
 * в null — для вызывающего это «обновления нет».
 *
 * Бета публикуется тегом `b1.21.1.2` с флагом pre-release; её версия получает суффикс `-beta`
 * (`1.21.1.2-beta`), как `versionName` бета-сборки, чтобы стабильная той же версии считалась новее.
 */
internal fun GitHubReleaseDto.toDomain(): AppRelease? {
    if (draft) return null
    val apkUrl = (assets.firstOrNull { it.browserDownloadUrl.endsWith(".apk", ignoreCase = true) } ?: assets.firstOrNull())
        ?.browserDownloadUrl
        ?: return null
    return AppRelease(
        version = domainVersion(),
        changelog = body.orEmpty(),
        apkUrl = apkUrl,
        isPrerelease = isPrerelease(),
    )
}

/** Список изменений релиза для истории версий; APK не требуется, черновик маппится в null. */
internal fun GitHubReleaseDto.toReleaseNotes(): AppReleaseNotes? {
    if (draft) return null
    return AppReleaseNotes(
        version = domainVersion(),
        publishedDate = publishedAt?.takeIf { it.length >= ISO_DATE_LENGTH }?.take(ISO_DATE_LENGTH),
        changelog = body.orEmpty(),
        isPrerelease = isPrerelease(),
    )
}

private fun GitHubReleaseDto.isPrerelease(): Boolean = prerelease || tagName.startsWith(BETA_TAG_PREFIX)

private fun GitHubReleaseDto.domainVersion(): String {
    val version = tagName.removePrefix(STABLE_TAG_PREFIX.toString()).removePrefix(BETA_TAG_PREFIX.toString())
    return if (isPrerelease() && '-' !in version) version + PRERELEASE_VERSION_SUFFIX else version
}
