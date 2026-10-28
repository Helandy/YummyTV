package su.afk.yummy.tv.source.animevost.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import java.io.IOException
import javax.inject.Inject

/** Сайт animevost.org: страница тайтла и плеер серии. Заголовков не нужно: страницы и mp4 отвечают без Referer. */
internal class AnimevostApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
) {
    /** Текст ответа; `null` при 404: страницы или серии там нет. */
    suspend fun getText(url: String): String? {
        val response = client.get(url)
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) throw IOException("AnimeVost: HTTP ${response.status.value}")
        return response.bodyAsText()
    }
}
