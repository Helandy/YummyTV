package su.afk.yummy.tv.source.dreamerscast.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import java.io.IOException
import javax.inject.Inject

/** Сайт dreamerscast.com (страница релиза с плеером) и HLS-плейлисты play.dreamerscast.com. Заголовков не нужно. */
internal class DreamerscastApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
) {
    /** Текст ответа; `null` при 404. */
    suspend fun getText(url: String): String? {
        val response = client.get(url)
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) throw IOException("Dreamerscast: HTTP ${response.status.value}")
        return response.bodyAsText()
    }
}
