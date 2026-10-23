package su.afk.yummy.tv.source.amedia.data.network

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
import su.afk.yummy.tv.source.amedia.data.dto.AmediaLookupRequestDto
import su.afk.yummy.tv.source.amedia.data.dto.AmediaTitleResponseDto
import java.io.IOException
import javax.inject.Inject

/** Сервер YummyTV (какая страница amd.online у тайтла), страница тайтла, страница плеера `aser.pro/vod/{id}` и HLS-плейлисты. */
internal class AmediaApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
    private val signers: Set<@JvmSuppressWildcards SiteRequestSigner>,
    private val siteApiConfig: SiteApiConfig,
) {
    suspend fun findTitle(request: AmediaLookupRequestDto): AmediaTitleResponseDto {
        val url = "${siteApiConfig.baseUrl}/amedia/title"
        // Тело сериализуется один раз: подписанные байты совпадают с отправленными.
        val body = Json.encodeToString(AmediaLookupRequestDto.serializer(), request)
        val response = client.post(url) {
            signSiteRequest(signers, "POST", url, body.toByteArray(Charsets.UTF_8))
            setBody(TextContent(body, ContentType.Application.Json))
        }
        if (!response.status.isSuccess()) throw IOException("Amedia API: HTTP ${response.status.value}")
        return response.body()
    }

    suspend fun getTitlePage(pageUrl: String): String = getText(pageUrl)

    suspend fun getVodPage(vodId: Long): String = getText("$VOD_URL/$vodId")

    suspend fun getPlaylist(url: String): String = getText(url)

    private suspend fun getText(url: String, block: HttpRequestBuilder.() -> Unit = {}): String {
        val response = client.get(url, block)
        if (!response.status.isSuccess()) throw IOException("Amedia: HTTP ${response.status.value}")
        return response.bodyAsText()
    }

    private companion object {
        const val VOD_URL = "https://aser.pro/vod"
    }
}
