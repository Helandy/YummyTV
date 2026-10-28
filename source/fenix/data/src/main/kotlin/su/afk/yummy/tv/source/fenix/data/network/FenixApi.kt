package su.afk.yummy.tv.source.fenix.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
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
}
