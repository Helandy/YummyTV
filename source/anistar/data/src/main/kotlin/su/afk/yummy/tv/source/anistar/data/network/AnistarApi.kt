package su.afk.yummy.tv.source.anistar.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import su.afk.yummy.tv.core.network.yummy.SiteApiConfig
import su.afk.yummy.tv.source.anistar.data.dto.AnistarMirrorResponseDto
import java.io.IOException
import javax.inject.Inject

/** Собственный сервер YummyTV (живое зеркало; наличие тайтла и серии приходят из общей ручки наличия) и плеер-страница AniStar (потоки серии). */
internal class AnistarApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
    private val siteApiConfig: SiteApiConfig,
) {
    suspend fun getMirror(): AnistarMirrorResponseDto {
        val url = "${siteApiConfig.baseUrl}/anistar/mirror"
        val response = client.get(url)
        if (!response.status.isSuccess()) throw IOException("AniStar API: HTTP ${response.status.value}")
        return response.body()
    }

    /** HTML плеер-страницы тайтла с JS-массивом серий и ссылками на потоки; для несуществующей новости сайт отвечает 200 без серий. */
    suspend fun getPlayerPage(mirror: String, newsId: Long): String {
        val response = client.get("$mirror/test/player2/videoas_p2p_new.php") { url.parameters.append("id", newsId.toString()) }
        if (!response.status.isSuccess()) throw IOException("AniStar player: HTTP ${response.status.value}")
        return response.bodyAsText()
    }
}
