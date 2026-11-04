package su.afk.yummy.tv.source.studiomir.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import java.io.IOException
import javax.inject.Inject

/** Сайт studiomir.club: страница тайтла, в HTML которой лежит плейлист плеера TSM. */
internal class StudioMirApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
) {
    /** Текст страницы; `null` при 404: тайтла на сайте нет. */
    suspend fun getText(url: String): String? {
        val response = client.get(url)
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) throw IOException("StudioMir: HTTP ${response.status.value}")
        return response.bodyAsText()
    }
}
