package su.afk.yummy.tv.source.torrent.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import su.afk.yummy.tv.core.network.di.TorrentHttpClient
import su.afk.yummy.tv.core.preferences.settings.TorrentSettingsStore
import su.afk.yummy.tv.source.torrent.data.dto.TorrServerAddRequestDto
import su.afk.yummy.tv.source.torrent.data.dto.TorrServerTorrentDto
import su.afk.yummy.tv.source.torrent.data.mapper.toDomain
import su.afk.yummy.tv.source.torrent.data.mapper.toProgress
import su.afk.yummy.tv.source.torrent.data.utils.basicAuthHeaders
import su.afk.yummy.tv.source.torrent.data.utils.normalizeBaseUrl
import su.afk.yummy.tv.source.torrent.data.utils.torrentJson
import su.afk.yummy.tv.source.torrent.data.utils.urlEncoded
import su.afk.yummy.tv.source.torrent.domain.model.TorrentContent
import su.afk.yummy.tv.source.torrent.domain.model.TorrentFile
import su.afk.yummy.tv.source.torrent.domain.model.TorrentMetadataUnavailableException
import su.afk.yummy.tv.source.torrent.domain.model.TorrentProgress
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

    override suspend fun addTorrent(link: String, title: String, onProgress: (TorrentProgress) -> Unit): TorrentContent {
        val settings = settingsStore.torrentSettings.first()
        val base = normalizeBaseUrl(settings.torrServerUrl)
        if (base.isEmpty()) throw IOException("TorrServer address is empty")
        val headers = basicAuthHeaders(settings.torrServerUser, settings.torrServerPassword)

        var dto = post(base, headers, TorrServerAddRequestDto(action = "add", link = link, title = title.takeIf { it.isNotBlank() }))
        var content = dto.toDomain() ?: throw IOException("TorrServer returned no hash")
        // Метаданные приходят не сразу: пока список файлов пуст, переспрашиваем состояние по хешу.
        var attempts = 0
        while (content.files.isEmpty() && attempts < METADATA_POLL_ATTEMPTS) {
            delay(METADATA_POLL_DELAY_MS)
            dto = post(base, headers, TorrServerAddRequestDto(action = "get", hash = content.hash))
            content = dto.toDomain() ?: content
            onProgress(dto.toProgress())
            attempts++
        }
        // Файлов так и нет: пиры не нашлись. Это не «пустая раздача», поэтому отдельная ошибка.
        if (content.files.isEmpty()) throw TorrentMetadataUnavailableException(content.hash)
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

    override suspend fun awaitStreamData(stream: TorrentStream): Boolean = try {
        val response = client.get(stream.url) {
            stream.headers.forEach { (name, value) -> header(name, value) }
            header(HttpHeaders.Range, "bytes=0-${WARMUP_BYTES - 1}")
            timeout {
                requestTimeoutMillis = WARMUP_TIMEOUT_MS
                socketTimeoutMillis = WARMUP_TIMEOUT_MS
            }
        }
        response.status.isSuccess()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    override suspend fun knownHashes(): Set<String> = try {
        val settings = settingsStore.torrentSettings.first()
        val base = normalizeBaseUrl(settings.torrServerUrl)
        if (base.isEmpty()) {
            emptySet()
        } else {
            val response = client.post("$base/torrents") {
                basicAuthHeaders(settings.torrServerUser, settings.torrServerPassword)
                    .forEach { (name, value) -> header(name, value) }
                contentType(ContentType.Application.Json)
                setBody(torrentJson.encodeToString(TorrServerAddRequestDto(action = "list")))
                timeout { requestTimeoutMillis = LIST_TIMEOUT_MS }
            }
            if (response.status.isSuccess()) {
                torrentJson.decodeFromString<List<TorrServerTorrentDto>>(response.bodyAsText())
                    .mapNotNull { it.hash?.lowercase()?.takeIf(String::isNotBlank) }
                    .toSet()
            } else {
                emptySet()
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptySet()
    }

    private suspend fun post(
        base: String,
        headers: Map<String, String>,
        request: TorrServerAddRequestDto,
    ): TorrServerTorrentDto {
        val response: HttpResponse = client.post("$base/torrents") {
            headers.forEach { (name, value) -> header(name, value) }
            contentType(ContentType.Application.Json)
            setBody(torrentJson.encodeToString(request))
            // Добавление ждёт метаданные с трекеров: дольше общего профиля API.
            timeout { requestTimeoutMillis = ADD_TIMEOUT_MS }
        }
        if (!response.status.isSuccess()) throw IOException("TorrServer HTTP ${response.status.value}")
        return torrentJson.decodeFromString<TorrServerTorrentDto>(response.bodyAsText())
    }

    private companion object {
        const val ADD_TIMEOUT_MS = 120_000L
        const val LIST_TIMEOUT_MS = 5_000L

        /** Сколько ждём первых данных с нуля: поиск пиров на старте раздачи занимает до минуты и больше. */
        const val WARMUP_TIMEOUT_MS = 120_000L
        const val WARMUP_BYTES = 1_048_576
        const val METADATA_POLL_ATTEMPTS = 30
        const val METADATA_POLL_DELAY_MS = 2_000L
    }
}
