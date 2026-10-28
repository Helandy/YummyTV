package su.afk.yummy.tv.source.ailiberty.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import java.io.IOException
import javax.inject.Inject

/** Сайт ailiberty.top: страница релиза со скриптом плеера. Заголовков не нужно: плейлисты отдаются и без Referer. */
internal class AiLibertyApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
) {
    /** HTML страницы релиза; `null` при 404: релиза на сайте больше нет. */
    suspend fun getReleasePage(url: String): String? {
        val response = client.get(url)
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) throw IOException("AiLiberty page: HTTP ${response.status.value}")
        return response.bodyAsText()
    }
}
