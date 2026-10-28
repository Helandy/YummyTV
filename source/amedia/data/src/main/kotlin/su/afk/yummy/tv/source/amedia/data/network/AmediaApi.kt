package su.afk.yummy.tv.source.amedia.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import java.io.IOException
import javax.inject.Inject

/** Страница плеера `aser.pro/vod/{id}` и HLS-плейлисты; страницу тайтла и серии приложение берёт из общей ручки наличия на сервере. */
internal class AmediaApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
) {
    suspend fun getVodPage(vodId: Long): String = getText("$VOD_URL/$vodId")

    suspend fun getPlaylist(url: String): String = getText(url)

    private suspend fun getText(url: String): String {
        val response = client.get(url)
        if (!response.status.isSuccess()) throw IOException("Amedia: HTTP ${response.status.value}")
        return response.bodyAsText()
    }

    private companion object {
        const val VOD_URL = "https://aser.pro/vod"
    }
}
