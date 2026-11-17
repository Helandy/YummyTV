package su.afk.yummy.tv.source.torrent.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.core.model.settings.TorrServerCheckResult
import su.afk.yummy.tv.core.model.settings.TorrServerChecker
import su.afk.yummy.tv.core.model.settings.TorrentIndexer
import su.afk.yummy.tv.core.model.settings.TorrentIndexerCheckResult
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

    /**
     * JacRed — тем же запросом, что и поиск (короткая строка); Jackett — лёгким `t=caps` без опроса трекеров,
     * он же проверяет API-ключ: при неверном ключе отвечает `<error code="100">`.
     */
    override suspend fun checkIndexer(settings: TorrentSettings, indexer: TorrentIndexer): TorrentIndexerCheckResult {
        val base = normalizeBaseUrl(
            if (indexer == TorrentIndexer.JACRED) settings.effectiveJacRedUrl else settings.jackettUrl,
        )
        if (base.isEmpty()) return TorrentIndexerCheckResult.Unreachable
        return try {
            val response = when (indexer) {
                TorrentIndexer.JACRED -> client.get("$base/api/v1.0/torrents") {
                    parameter("search", INDEXER_PROBE_QUERY)
                    timeout { requestTimeoutMillis = INDEXER_CHECK_TIMEOUT_MS }
                }
                TorrentIndexer.JACKETT -> client.get("$base/api/v2.0/indexers/all/results/torznab/api") {
                    parameter("apikey", settings.jackettApiKey)
                    parameter("t", "caps")
                    timeout { requestTimeoutMillis = INDEXER_CHECK_TIMEOUT_MS }
                }
            }
            val code = response.status.value
            when {
                response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden ->
                    TorrentIndexerCheckResult.Unauthorized
                code !in 200..299 -> TorrentIndexerCheckResult.UnexpectedResponse(code)
                indexer == TorrentIndexer.JACRED -> TorrentIndexerCheckResult.Ok
                else -> response.bodyAsText().jackettCapsResult(code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            TorrentIndexerCheckResult.Unreachable
        }
    }

    private fun String.jackettCapsResult(code: Int): TorrentIndexerCheckResult = when {
        contains("<caps", ignoreCase = true) -> TorrentIndexerCheckResult.Ok
        contains("code=\"1", ignoreCase = true) -> TorrentIndexerCheckResult.Unauthorized
        else -> TorrentIndexerCheckResult.UnexpectedResponse(code)
    }

    private companion object {
        const val INDEXER_PROBE_QUERY = "naruto"
        const val INDEXER_CHECK_TIMEOUT_MS = 20_000L
        const val CHECK_TIMEOUT_MS = 8_000L
        const val MAX_VERSION_LENGTH = 40
    }
}
