package su.afk.yummy.tv.source.torrent.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.model.settings.TorrentIndexer
import su.afk.yummy.tv.core.network.di.TorrentHttpClient
import su.afk.yummy.tv.core.preferences.settings.TorrentSettingsStore
import su.afk.yummy.tv.source.torrent.data.dto.JacRedTorrentDto
import su.afk.yummy.tv.source.torrent.data.dto.JackettResponseDto
import su.afk.yummy.tv.source.torrent.data.mapper.toDomain
import su.afk.yummy.tv.source.torrent.data.mapper.toJacRedDomain
import su.afk.yummy.tv.source.torrent.data.utils.normalizeBaseUrl
import su.afk.yummy.tv.source.torrent.data.utils.torrentJson
import su.afk.yummy.tv.source.torrent.domain.model.TorrentResult
import su.afk.yummy.tv.source.torrent.domain.repository.TorrentIndexerRepository
import java.io.IOException
import javax.inject.Inject

/** Ходит в JacRed (`/api/v1.0/torrents?search=`) или Jackett (`/api/v2.0/indexers/all/results`) по настройке. */
internal class TorrentIndexerRepositoryImpl @Inject constructor(
    @TorrentHttpClient private val client: HttpClient,
    private val settingsStore: TorrentSettingsStore,
) : TorrentIndexerRepository {

    override suspend fun search(query: String): List<TorrentResult> {
        val settings = settingsStore.torrentSettings.first()
        val base = normalizeBaseUrl(settings.indexerUrl)
        return when (settings.indexer) {
            TorrentIndexer.JACRED -> {
                val body = client.get("$base/api/v1.0/torrents") {
                    parameter("search", query)
                }.requireBody()
                torrentJson.decodeFromString<List<JacRedTorrentDto>>(body).toJacRedDomain()
            }

            TorrentIndexer.JACKETT -> {
                val body = client.get("$base/api/v2.0/indexers/all/results") {
                    parameter("apikey", settings.jackettApiKey)
                    parameter("Query", query)
                }.requireBody()
                torrentJson.decodeFromString<JackettResponseDto>(body).toDomain()
            }
        }
    }

    private suspend fun HttpResponse.requireBody(): String {
        if (!status.isSuccess()) throw IOException("indexer HTTP ${status.value}")
        return bodyAsText()
    }
}
