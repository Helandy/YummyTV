package su.afk.yummy.tv.source.fenix.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import java.io.IOException
import javax.inject.Inject

/** Страницы плеера Fenix. Заголовков не нужно: страница и медиа-хост отвечают без Referer, доступ даёт токен со страницы. */
internal class FenixApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
) {
    /** Текст страницы; `null` при 404. */
    suspend fun getText(url: String): String? {
        val response = client.get(url)
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) throw IOException("Fenix: HTTP ${response.status.value}")
        return response.bodyAsText()
    }

    /** Тело ответа `/api/playback/…` с токеном страницы в заголовке `X-Fenix-Playback`; `null` при 404. */
    suspend fun getPlayback(path: String, token: String): String? {
        val response = client.get("https://fenixplay.xyz$path") { header("X-Fenix-Playback", token) }
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) throw IOException("Fenix: HTTP ${response.status.value}")
        return response.bodyAsText()
    }

    /** Отвечает ли [url] успешно с первым байтом за [timeoutMs]; сеть и таймаут — `false`, а не ошибка. */
    suspend fun isReachable(url: String, timeoutMs: Long): Boolean =
        try {
            withTimeout(timeoutMs) { client.get(url).status.isSuccess() }
        } catch (_: TimeoutCancellationException) {
            false
        } catch (_: IOException) {
            false
        }
}
