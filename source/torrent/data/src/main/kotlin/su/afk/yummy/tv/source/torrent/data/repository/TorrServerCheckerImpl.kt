package su.afk.yummy.tv.source.torrent.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.core.model.settings.TorrServerCheckResult
import su.afk.yummy.tv.core.model.settings.TorrServerChecker
import su.afk.yummy.tv.core.model.settings.TorrentSettings
import su.afk.yummy.tv.core.network.di.TorrentHttpClient
import su.afk.yummy.tv.source.torrent.data.utils.basicAuthHeaders
import su.afk.yummy.tv.source.torrent.data.utils.normalizeBaseUrl
import javax.inject.Inject

/** Проверяет TorrServer запросом `GET /echo`: он отвечает версией сервера и требует авторизацию, если она включена. */
internal class TorrServerCheckerImpl @Inject constructor(
    @TorrentHttpClient private val client: HttpClient,
) : TorrServerChecker {

    override suspend fun check(settings: TorrentSettings): TorrServerCheckResult {
        val base = normalizeBaseUrl(settings.torrServerUrl)
        if (base.isEmpty()) return TorrServerCheckResult.Unreachable
        return try {
            val response = client.get("$base/echo") {
                basicAuthHeaders(settings.torrServerUser, settings.torrServerPassword)
                    .forEach { (name, value) -> header(name, value) }
                timeout { requestTimeoutMillis = CHECK_TIMEOUT_MS }
            }
            when {
                response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden ->
                    TorrServerCheckResult.Unauthorized
                response.status.value in 200..299 -> TorrServerCheckResult.Ok(response.bodyAsText().trim().take(MAX_VERSION_LENGTH))
                else -> TorrServerCheckResult.UnexpectedResponse(response.status.value)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            TorrServerCheckResult.Unreachable
        }
    }

    private companion object {
        const val CHECK_TIMEOUT_MS = 8_000L
        const val MAX_VERSION_LENGTH = 40
    }
}
