package su.afk.yummy.tv.source.anime4k.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import su.afk.yummy.tv.core.network.yummy.SiteApiConfig
import su.afk.yummy.tv.core.network.yummy.signSiteRequest
import su.afk.yummy.tv.source.anime4k.data.dto.Anime4kStreamsRequestDto
import su.afk.yummy.tv.source.anime4k.data.dto.Anime4kStreamsResponseDto
import su.afk.yummy.tv.source.anime4k.data.dto.Anime4kTicketResponseDto
import su.afk.yummy.tv.source.anime4k.data.dto.Anime4kTitleResponseDto
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kApiException
import su.afk.yummy.tv.source.anime4k.data.signing.RequestSigner
import java.io.IOException
import javax.inject.Inject

/** Собственный сервер YummyTV: наличие 4K без рекламы, ссылки только по тикету. */
internal class Anime4kApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
    private val signer: RequestSigner,
    private val siteApiConfig: SiteApiConfig = SiteApiConfig(),
) {
    private val base: String get() = "${siteApiConfig.baseUrl}/anime4k"

    /** `null` при 404: тайтла на сервере нет. */
    suspend fun getTitle(malId: Int): Anime4kTitleResponseDto? {
        val url = "$base/title/$malId"
        val response = client.get(url) {
            sign("GET", url, ByteArray(0))
        }
        if (response.status == HttpStatusCode.NotFound) return null
        response.requireSuccess()
        return response.body()
    }

    suspend fun requestTicket(deviceId: String?): Anime4kTicketResponseDto {
        val url = "$base/ticket"
        val response = client.post(url) {
            deviceId?.let { header(DEVICE_HEADER, it) }
            sign("POST", url, ByteArray(0))
        }
        response.requireSuccess()
        return response.body()
    }

    suspend fun getStreams(request: Anime4kStreamsRequestDto, deviceId: String?): Anime4kStreamsResponseDto {
        val url = "$base/streams"
        // Тело сериализуется один раз: подписанные байты совпадают с отправленными.
        val body = Json.encodeToString(Anime4kStreamsRequestDto.serializer(), request)
        val response = client.post(url) {
            deviceId?.let { header(DEVICE_HEADER, it) }
            sign("POST", url, body.toByteArray(Charsets.UTF_8))
            setBody(TextContent(body, ContentType.Application.Json))
        }
        response.requireSuccess()
        return response.body()
    }

    private fun HttpRequestBuilder.sign(method: String, url: String, body: ByteArray) {
        signSiteRequest(listOf(signer), method, url, body)
    }

    private fun io.ktor.client.statement.HttpResponse.requireSuccess() {
        when {
            status.isSuccess() -> Unit
            status == HttpStatusCode.TooManyRequests -> throw Anime4kApiException.RateLimited()
            status == HttpStatusCode.Forbidden || status == HttpStatusCode.Unauthorized ->
                throw Anime4kApiException.Rejected()
            status == HttpStatusCode.NotFound -> throw Anime4kApiException.NotFound()
            else -> throw IOException("Anime4k API: HTTP ${status.value}")
        }
    }

    private companion object {
        const val DEVICE_HEADER = "X-Device-Id"
    }
}
