package su.afk.yummy.tv.data.update.mapper

import su.afk.yummy.tv.data.update.dto.GitHubReleaseDto
import su.afk.yummy.tv.domain.update.model.AppRelease
import su.afk.yummy.tv.domain.update.util.PRERELEASE_VERSION_SUFFIX

private const val STABLE_TAG_PREFIX = 'v'
private const val BETA_TAG_PREFIX = 'b'

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
    val isPrerelease = prerelease || tagName.startsWith(BETA_TAG_PREFIX)
    val version = tagName.removePrefix(STABLE_TAG_PREFIX.toString()).removePrefix(BETA_TAG_PREFIX.toString())
    return AppRelease(
        version = if (isPrerelease && '-' !in version) version + PRERELEASE_VERSION_SUFFIX else version,
        changelog = body.orEmpty(),
        apkUrl = apkUrl,
        isPrerelease = isPrerelease,
    )
}
