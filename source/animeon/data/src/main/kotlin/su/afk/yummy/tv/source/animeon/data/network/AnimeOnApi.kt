package su.afk.yummy.tv.source.animeon.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import java.io.IOException
import javax.inject.Inject

/** API сайта animeon.club и страницы его плееров. Заголовков не нужно: плеер Ashdi и его CDN отвечают без Referer. */
internal class AnimeOnApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
) {
    /** Текст ответа; `null` при 404: серии или страницы там нет. */
    suspend fun getText(url: String): String? {
        val response = client.get(url)
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) throw IOException("AnimeON: HTTP ${response.status.value}")
        return response.bodyAsText()
    }
}
