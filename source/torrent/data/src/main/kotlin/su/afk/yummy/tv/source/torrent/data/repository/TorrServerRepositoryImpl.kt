package su.afk.yummy.tv.source.torrent.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import su.afk.yummy.tv.core.network.di.TorrentHttpClient
import su.afk.yummy.tv.core.preferences.settings.TorrentSettingsStore
import su.afk.yummy.tv.source.torrent.data.dto.TorrServerAddRequestDto
import su.afk.yummy.tv.source.torrent.data.dto.TorrServerTorrentDto
import su.afk.yummy.tv.source.torrent.data.mapper.toDomain
import su.afk.yummy.tv.source.torrent.data.utils.basicAuthHeaders
import su.afk.yummy.tv.source.torrent.data.utils.normalizeBaseUrl
import su.afk.yummy.tv.source.torrent.data.utils.torrentJson
import su.afk.yummy.tv.source.torrent.data.utils.urlEncoded
import su.afk.yummy.tv.source.torrent.domain.model.TorrentContent
import su.afk.yummy.tv.source.torrent.domain.model.TorrentFile
import su.afk.yummy.tv.source.torrent.domain.model.TorrentStream
import su.afk.yummy.tv.source.torrent.domain.model.fileName
import su.afk.yummy.tv.source.torrent.domain.repository.TorrServerRepository
import java.io.IOException
import javax.inject.Inject

/** TorrServer: `POST /torrents` добавляет раздачу и отдаёт файлы, поток играется с `GET /stream/{имя}?link={hash}&index={n}&play`. */
internal class TorrServerRepositoryImpl @Inject constructor(
    @TorrentHttpClient private val client: HttpClient,
    private val settingsStore: TorrentSettingsStore,
) : TorrServerRepository {

    override suspend fun addTorrent(link: String): TorrentContent {
        val settings = settingsStore.torrentSettings.first()
        val base = normalizeBaseUrl(settings.torrServerUrl)
        if (base.isEmpty()) throw IOException("TorrServer address is empty")
        val headers = basicAuthHeaders(settings.torrServerUser, settings.torrServerPassword)

        var content = post(base, headers, TorrServerAddRequestDto(action = "add", link = link))
        // Метаданные приходят не сразу: пока список файлов пуст, переспрашиваем состояние по хешу.
        var attempts = 0
        while (content.files.isEmpty() && attempts < METADATA_POLL_ATTEMPTS) {
            delay(METADATA_POLL_DELAY_MS)
            content = post(base, headers, TorrServerAddRequestDto(action = "get", hash = content.hash))
            attempts++
        }
        return content
    }

    override suspend fun streamFor(hash: String, file: TorrentFile): TorrentStream {
        val settings = settingsStore.torrentSettings.first()
        val base = normalizeBaseUrl(settings.torrServerUrl)
        val name = file.fileName.ifBlank { "video" }.urlEncoded()
        return TorrentStream(
            url = "$base/stream/$name?link=${hash.urlEncoded()}&index=${file.index}&play",
            headers = basicAuthHeaders(settings.torrServerUser, settings.torrServerPassword),
        )
    }

    private suspend fun post(
        base: String,
        headers: Map<String, String>,
        request: TorrServerAddRequestDto,
    ): TorrentContent {
        val response: HttpResponse = client.post("$base/torrents") {
            headers.forEach { (name, value) -> header(name, value) }
            contentType(ContentType.Application.Json)
            setBody(torrentJson.encodeToString(request))
            // Добавление ждёт метаданные с трекеров: дольше общего профиля API.
            timeout { requestTimeoutMillis = ADD_TIMEOUT_MS }
        }
        if (!response.status.isSuccess()) throw IOException("TorrServer HTTP ${response.status.value}")
        val dto = torrentJson.decodeFromString<TorrServerTorrentDto>(response.bodyAsText())
        return dto.toDomain() ?: throw IOException("TorrServer returned no hash")
    }

    private companion object {
        const val ADD_TIMEOUT_MS = 60_000L
        const val METADATA_POLL_ATTEMPTS = 15
        const val METADATA_POLL_DELAY_MS = 2_000L
    }
}
