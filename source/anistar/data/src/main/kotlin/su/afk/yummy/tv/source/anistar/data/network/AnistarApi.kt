package su.afk.yummy.tv.source.anistar.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import su.afk.yummy.tv.core.network.yummy.SiteApiConfig
import su.afk.yummy.tv.core.network.yummy.SiteRequestSigner
import su.afk.yummy.tv.core.network.yummy.signSiteRequest
import su.afk.yummy.tv.source.anistar.data.dto.AnistarLookupRequestDto
import su.afk.yummy.tv.source.anistar.data.dto.AnistarMirrorResponseDto
import su.afk.yummy.tv.source.anistar.data.dto.AnistarTitleResponseDto
import java.io.IOException
import javax.inject.Inject

/** Собственный сервер YummyTV (есть ли тайтл на AniStar, живое зеркало) и плеер-страница AniStar (серии и потоки). */
internal class AnistarApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
    private val signers: Set<@JvmSuppressWildcards SiteRequestSigner>,
    private val siteApiConfig: SiteApiConfig,
) {
    suspend fun findTitle(request: AnistarLookupRequestDto): AnistarTitleResponseDto {
        val url = "${siteApiConfig.baseUrl}/anistar/title"
        // Тело сериализуется один раз: подписанные байты совпадают с отправленными.
        val body = Json.encodeToString(AnistarLookupRequestDto.serializer(), request)
        val response = client.post(url) {
            sign("POST", url, body.toByteArray(Charsets.UTF_8))
            setBody(TextContent(body, ContentType.Application.Json))
        }
        if (!response.status.isSuccess()) throw IOException("AniStar API: HTTP ${response.status.value}")
        return response.body()
    }

    suspend fun getMirror(): AnistarMirrorResponseDto {
        val url = "${siteApiConfig.baseUrl}/anistar/mirror"
        val response = client.get(url) { sign("GET", url, ByteArray(0)) }
        if (!response.status.isSuccess()) throw IOException("AniStar API: HTTP ${response.status.value}")
        return response.body()
    }

    /** HTML плеер-страницы тайтла с JS-массивом серий; для несуществующей новости сайт отвечает 200 без серий. */
    suspend fun getPlayerPage(mirror: String, newsId: Long): String {
        val response = client.get("$mirror/test/player2/videoas_p2p_new.php") { url.parameters.append("id", newsId.toString()) }
        if (!response.status.isSuccess()) throw IOException("AniStar player: HTTP ${response.status.value}")
        return response.bodyAsText()
    }

    private fun HttpRequestBuilder.sign(method: String, url: String, body: ByteArray) {
        signSiteRequest(signers, method, url, body)
    }
}
