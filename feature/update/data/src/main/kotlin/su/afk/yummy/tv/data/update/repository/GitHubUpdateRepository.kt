package su.afk.yummy.tv.data.update.repository

import su.afk.yummy.tv.domain.update.model.ReleasesUnavailableException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import su.afk.yummy.tv.data.update.UpdateConfig
import su.afk.yummy.tv.data.update.dto.GitHubReleaseDto
import su.afk.yummy.tv.data.update.mapper.toDomain
import su.afk.yummy.tv.data.update.mapper.toReleaseNotes
import su.afk.yummy.tv.domain.update.model.AppRelease
import su.afk.yummy.tv.domain.update.model.AppReleaseNotes
import su.afk.yummy.tv.domain.update.repository.UpdateRepository
import su.afk.yummy.tv.domain.update.utils.compareVersions
import su.afk.yummy.tv.domain.update.utils.isVersionNewer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import su.afk.yummy.tv.core.featuretoggle.api.FeatureFlags
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleProvider
import javax.inject.Inject

/**
 * Релизы из GitHub Releases: самый новый для проверки обновлений и история изменений
 * (pre-release — только для бета-канала).
 * Клиент намеренно неавторизованный: api.github.com — публичный сторонний сервис, токен приложения ему отправлять незачем.
 */
internal class GitHubUpdateRepository @Inject constructor(
    @param:UnauthenticatedJsonClient private val httpClient: HttpClient,
    private val featureToggleProvider: FeatureToggleProvider,
) : UpdateRepository {

    override suspend fun latestRelease(currentVersion: String, includePrerelease: Boolean): AppRelease? {
        // GitHub недоступен (ошибка, таймаут, пустой ответ) — резервный релиз по прямой ссылке из флагов.
        val releases = fetchReleasesOrNull()
            ?.mapNotNull { it.toDomain() }
            ?.filter { includePrerelease || !it.isPrerelease }
            ?.takeIf { it.isNotEmpty() }
            ?: return fallbackRelease(currentVersion)
        val latest = releases.maxWithOrNull { a, b -> compareVersions(a.version, b.version) }
            ?: return fallbackRelease(currentVersion)
        val updatesCount = releases.count { isVersionNewer(currentVersion, it.version) }
        return latest.copy(updatesCount = updatesCount)
    }

    override suspend fun releaseHistory(currentVersion: String, includePrerelease: Boolean): List<AppReleaseNotes> =
        (fetchReleases() ?: throw ReleasesUnavailableException())
            .mapNotNull { it.toReleaseNotes() }
            .filter { (includePrerelease || !it.isPrerelease) && !isVersionNewer(currentVersion, it.version) }
            .sortedWith { a, b -> compareVersions(b.version, a.version) }

    /** Короче общего таймаута проверки в MainSideEffectsHandler, чтобы на резерв осталось время. */
    private suspend fun fetchReleasesOrNull(): List<GitHubReleaseDto>? =
        try {
            withTimeoutOrNull(GITHUB_TIMEOUT_MS) { fetchReleases() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    private fun fallbackRelease(currentVersion: String): AppRelease? {
        val url = featureToggleProvider.getString(FeatureFlags.updateFallbackApkUrl).trim()
        val version = featureToggleProvider.getString(FeatureFlags.updateFallbackVersion).trim()
        if (!url.startsWith("https://", ignoreCase = true) || version.isBlank()) return null
        return AppRelease(
            version = version,
            changelog = featureToggleProvider.getString(FeatureFlags.updateFallbackChangelog).trim(),
            apkUrl = url,
            updatesCount = if (isVersionNewer(currentVersion, version)) 1 else 0,
        )
    }

    /** null, когда репозиторий обновлений не сконфигурирован или GitHub ответил ошибкой. */
    private suspend fun fetchReleases(): List<GitHubReleaseDto>? {
        val url = RELEASES_URL ?: return null
        val response: HttpResponse = httpClient.get(url) {
            header("Accept", GITHUB_ACCEPT)
            parameter("per_page", RELEASES_PER_PAGE)
        }
        if (!response.status.isSuccess()) return null
        return response.body<List<GitHubReleaseDto>>()
    }

    private companion object {
        const val GITHUB_TIMEOUT_MS = 3_500L
        const val GITHUB_ACCEPT = "application/vnd.github+json"

        /** Максимум GitHub на страницу: беты идут часто и не должны вытеснять последний стабильный релиз. */
        const val RELEASES_PER_PAGE = 100

        /** null, когда репозиторий обновлений не сконфигурирован — проверка просто выключена. */
        val RELEASES_URL: String? =
            if (UpdateConfig.GITHUB_OWNER.isNotBlank() && UpdateConfig.GITHUB_REPO.isNotBlank()) {
                "https://api.github.com/repos/" +
                        "${UpdateConfig.GITHUB_OWNER}/${UpdateConfig.GITHUB_REPO}/releases"
            } else {
                null
            }
    }
}
