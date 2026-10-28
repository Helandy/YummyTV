package su.afk.yummy.tv.source.availability.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import su.afk.yummy.tv.core.network.yummy.SiteApiConfig
import su.afk.yummy.tv.core.network.yummy.SiteRequestSigner
import su.afk.yummy.tv.core.network.yummy.signSiteRequest
import su.afk.yummy.tv.source.availability.data.dto.AvailabilityRequestDto
import su.afk.yummy.tv.source.availability.data.dto.AvailabilityResponseDto
import java.io.IOException
import javax.inject.Inject

/** Собственный сервер YummyTV: на каких балансерах есть тайтл (`POST /availability`). */
internal class AvailabilityApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
    private val signers: Set<@JvmSuppressWildcards SiteRequestSigner>,
    private val siteApiConfig: SiteApiConfig,
) {
    suspend fun find(request: AvailabilityRequestDto): AvailabilityResponseDto {
        val url = "${siteApiConfig.baseUrl}/availability"
        // Тело сериализуется один раз: подписанные байты совпадают с отправленными.
        val body = Json.encodeToString(AvailabilityRequestDto.serializer(), request)
        val response = client.post(url) {
            sign("POST", url, body.toByteArray(Charsets.UTF_8))
            setBody(TextContent(body, ContentType.Application.Json))
        }
        if (!response.status.isSuccess()) throw IOException("Availability API: HTTP ${response.status.value}")
        return response.body()
    }

    private fun HttpRequestBuilder.sign(method: String, url: String, body: ByteArray) {
        signSiteRequest(signers, method, url, body)
    }
}
