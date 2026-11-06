package su.afk.yummy.tv.source.sameband.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import java.io.IOException
import javax.inject.Inject
import su.afk.yummy.tv.core.utils.network.HttpHeaderNames

/** Сайт sameband.studio: страница тайтла, страница плеера и JSON-плейлист серий. */
internal class SamebandApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
) {
    /** Текст ответа; `null` при 404. Сайт отвечает только с браузерным User-Agent и Referer страницы. */
    suspend fun getText(url: String, referer: String, userAgent: String): String? {
        val response = client.get(url) {
            header("Referer", referer)
            header(HttpHeaderNames.USER_AGENT, userAgent)
        }
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) throw IOException("Sameband: HTTP ${response.status.value}")
        return response.bodyAsText()
    }
}
