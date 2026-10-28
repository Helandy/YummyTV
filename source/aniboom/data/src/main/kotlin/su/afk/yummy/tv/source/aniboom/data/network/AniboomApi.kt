package su.afk.yummy.tv.source.aniboom.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import java.io.IOException
import javax.inject.Inject

/** Эмбед AniBoom (поток серии). Наличие тайтла приходит из общей ручки наличия, см. `:source:availability`. */
internal class AniboomApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
) {
    /** HTML страницы эмбеда; `null` при 404: серии или озвучки там нет. */
    suspend fun getEmbed(url: String, referer: String): String? {
        val response = client.get(url) { header("Referer", referer) }
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) throw IOException("Aniboom embed: HTTP ${response.status.value}")
        return response.bodyAsText()
    }

    /** Мастер-плейлист HLS. Заголовков нет намеренно: CDN отвечает 400 на запрос с `Referer`/`Origin`. */
    suspend fun getPlaylist(url: String): String {
        val response = client.get(url)
        if (!response.status.isSuccess()) throw IOException("Aniboom playlist: HTTP ${response.status.value}")
        return response.bodyAsText()
    }
}
