package su.afk.yummy.tv.source.aniboom.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import su.afk.yummy.tv.core.network.yummy.SiteApiConfig
import su.afk.yummy.tv.core.network.yummy.SiteRequestSigner
import su.afk.yummy.tv.core.network.yummy.signSiteRequest
import su.afk.yummy.tv.source.aniboom.data.dto.AniboomLookupRequestDto
import su.afk.yummy.tv.source.aniboom.data.dto.AniboomTitleResponseDto
import java.io.IOException
import javax.inject.Inject

/** Собственный сервер YummyTV (есть ли тайтл в AniBoom) и эмбед AniBoom (поток серии). */
internal class AniboomApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
    private val signers: Set<@JvmSuppressWildcards SiteRequestSigner>,
    private val siteApiConfig: SiteApiConfig,
) {
    suspend fun findTitle(request: AniboomLookupRequestDto): AniboomTitleResponseDto {
        val url = "${siteApiConfig.baseUrl}/aniboom/title"
        // Тело сериализуется один раз: подписанные байты совпадают с отправленными.
        val body = Json.encodeToString(AniboomLookupRequestDto.serializer(), request)
        val response = client.post(url) {
            sign("POST", url, body.toByteArray(Charsets.UTF_8))
            setBody(TextContent(body, ContentType.Application.Json))
        }
        if (!response.status.isSuccess()) throw IOException("Aniboom API: HTTP ${response.status.value}")
        return response.body()
    }

    /** HTML страницы эмбеда; `null` при 404: серии или озвучки там нет. */
    suspend fun getEmbed(url: String, referer: String): String? {
        val response = client.get(url) { header("Referer", referer) }
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) throw IOException("Aniboom embed: HTTP ${response.status.value}")
        return response.bodyAsText()
    }

    private fun HttpRequestBuilder.sign(method: String, url: String, body: ByteArray) {
        signSiteRequest(signers, method, url, body)
    }
}
