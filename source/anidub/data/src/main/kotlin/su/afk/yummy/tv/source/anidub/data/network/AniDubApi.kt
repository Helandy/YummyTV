package su.afk.yummy.tv.source.anidub.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import java.io.IOException
import javax.inject.Inject

/** Сайт online.anidub.com и его плеер: страница тайтла, плейлист серий и HLS-мастер серии. */
internal class AniDubApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
) {
    /**
     * Текст ответа; `null` при 404: страницы, плейлиста или серии там нет. [referer] нужен плейлисту плеера, [userAgent] —
     * запросам мастера и CDN: подпись ссылок привязана к нему.
     */
    suspend fun getText(url: String, referer: String? = null, userAgent: String? = null): String? {
        val response = client.get(url) {
            referer?.let { header("Referer", it) }
            userAgent?.let { header("User-Agent", it) }
        }
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) throw IOException("AniDub: HTTP ${response.status.value}")
        return response.bodyAsText()
    }
}
