package su.afk.yummy.tv.source.torrent.data.repository

import android.os.SystemClock
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.api.PersistedLogTags
import su.afk.yummy.tv.core.model.settings.TorrentIndexer
import su.afk.yummy.tv.core.model.settings.TorrentSettings
import su.afk.yummy.tv.core.network.di.TorrentHttpClient
import su.afk.yummy.tv.core.preferences.settings.TorrentSettingsStore
import su.afk.yummy.tv.source.torrent.data.dto.JacRedTorrentDto
import su.afk.yummy.tv.source.torrent.data.dto.JackettResponseDto
import su.afk.yummy.tv.source.torrent.data.mapper.toDomain
import su.afk.yummy.tv.source.torrent.data.mapper.toJacRedDomain
import su.afk.yummy.tv.source.torrent.data.utils.logSummary
import su.afk.yummy.tv.source.torrent.data.utils.normalizeBaseUrl
import su.afk.yummy.tv.source.torrent.data.utils.torrentJson
import su.afk.yummy.tv.source.torrent.domain.model.TorrentResult
import su.afk.yummy.tv.source.torrent.domain.repository.TorrentIndexerRepository
import java.io.IOException
import javax.inject.Inject

/** Ходит в JacRed (`/api/v1.0/torrents?search=`) и/или Jackett (`/api/v2.0/indexers/all/results`) — по тумблерам. */
internal class TorrentIndexerRepositoryImpl @Inject constructor(
    @TorrentHttpClient private val client: HttpClient,
    private val settingsStore: TorrentSettingsStore,
    private val analyticsTracker: AnalyticsTracker,
) : TorrentIndexerRepository {

    override suspend fun search(query: String): List<TorrentResult> {
        val settings = settingsStore.torrentSettings.first()
        val indexers = settings.activeIndexers
        if (indexers.isEmpty()) {
            analyticsTracker.log(LOG_TAG) { "Search skipped, no active indexer: ${settings.logSummary()}" }
            return emptyList()
        }
        // Включённые индексаторы опрашиваем параллельно; если часть упала, отдаём то, что нашли у остальных.
        val outcomes = coroutineScope {
            indexers.map { indexer -> async { timedSearch(indexer, settings, query) } }.awaitAll()
        }
        outcomes.firstNotNullOfOrNull { it.exceptionOrNull() as? CancellationException }?.let { throw it }
        if (outcomes.all { it.isFailure }) throw outcomes.last().exceptionOrNull()!!
        return outcomes.flatMap { it.getOrDefault(emptyList()) }
    }

    /** Поиск одним индексатором с записью в лог исхода и времени: без этого сбой индексатора никак не виден. */
    private suspend fun timedSearch(
        indexer: TorrentIndexer,
        settings: TorrentSettings,
        query: String,
    ): Result<List<TorrentResult>> {
        val startedAt = SystemClock.elapsedRealtime()
        return runCatching { search(indexer, settings, query) }
            .onSuccess { found ->
                analyticsTracker.log(LOG_TAG) {
                    "Search $indexer ok results=${found.size} queryLength=${query.length} " +
                        "tookMs=${SystemClock.elapsedRealtime() - startedAt}"
                }
            }
            .onFailure { error ->
                analyticsTracker.log(LOG_TAG) {
                    "Search $indexer failed ${error.logSummary()} tookMs=${SystemClock.elapsedRealtime() - startedAt}"
                }
            }
    }

    private suspend fun search(indexer: TorrentIndexer, settings: TorrentSettings, query: String): List<TorrentResult> =
        when (indexer) {
            TorrentIndexer.JACRED -> {
                val base = normalizeBaseUrl(settings.effectiveJacRedUrl)
                val body = client.get("$base/api/v1.0/torrents") {
                    parameter("search", query)
                    timeout { requestTimeoutMillis = SEARCH_TIMEOUT_MS }
                }.requireBody()
                torrentJson.decodeFromString<List<JacRedTorrentDto>>(body).toJacRedDomain()
            }

            TorrentIndexer.JACKETT -> {
                val base = normalizeBaseUrl(settings.jackettUrl)
                val body = client.get("$base/api/v2.0/indexers/all/results") {
                    parameter("apikey", settings.jackettApiKey)
                    parameter("Query", query)
                    timeout { requestTimeoutMillis = SEARCH_TIMEOUT_MS }
                }.requireBody()
                torrentJson.decodeFromString<JackettResponseDto>(body).toDomain()
            }
        }

    private companion object {
        const val LOG_TAG = PersistedLogTags.TORRENT

        /** Jackett без кэша опрашивает все свои индексаторы: первый поиск идёт до 15–20 с, общий профиль API (15 с) мал. */
        const val SEARCH_TIMEOUT_MS = 120_000L
    }

    private suspend fun HttpResponse.requireBody(): String {
        if (!status.isSuccess()) throw IOException("indexer HTTP ${status.value}")
        return bodyAsText()
    }
}
