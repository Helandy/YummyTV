package su.afk.yummy.tv.source.amedia.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import java.io.IOException
import javax.inject.Inject

/** Сайт amd.online (поиск и страницы тайтлов), страницы плеера `aser.pro/vod/{id}` и HLS-плейлисты; своего сервера не требуется. */
internal class AmediaApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
) {
    /** Выдача поиска: индекс сайта русскоязычный, по латинским названиям он ничего не находит. */
    suspend fun search(query: String): String = getText(SITE_URL) {
        url.parameters.append("do", "search")
        url.parameters.append("subaction", "search")
        url.parameters.append("story", query)
    }

    suspend fun getTitlePage(pageUrl: String): String = getText(pageUrl)

    suspend fun getVodPage(vodId: Long): String = getText("$VOD_URL/$vodId")

    suspend fun getPlaylist(url: String): String = getText(url)

    private suspend fun getText(
        url: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): String {
        val response = client.get(url, block)
        if (!response.status.isSuccess()) throw IOException("Amedia: HTTP ${response.status.value}")
        return response.bodyAsText()
    }

    private companion object {
        const val SITE_URL = "https://amd.online/"
        const val VOD_URL = "https://aser.pro/vod"
    }
}
