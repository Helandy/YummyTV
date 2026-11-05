package su.afk.yummy.tv.data.player.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpMethod
import io.ktor.util.toMap
import su.afk.yummy.tv.core.network.di.PlayerResolveClient
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

internal interface PlayerHttpClient {
    suspend fun getText(
        url: String,
        headers: Map<String, String> = emptyMap(),
        followRedirects: Boolean = true,
    ): PlayerHttpResponse

    suspend fun postText(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): PlayerHttpResponse

    suspend fun head(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): PlayerHttpResponse
}

internal data class PlayerHttpResponse(
    val statusCode: Int,
    val body: String,
    val headers: Map<String, List<String>>,
    val bodyBytes: ByteArray = body.toByteArray(),
) {
    val isSuccess: Boolean = statusCode in HTTP_OK_START..HTTP_OK_END
    val setCookieHeader: String
        get() = headers.entries
            .firstOrNull { it.key.equals("Set-Cookie", ignoreCase = true) }
            ?.value
            ?.joinToString("; ") { it.split(";").first() }
            .orEmpty()

    fun body(charset: Charset): String = bodyBytes.toString(charset)

    private companion object {
        const val HTTP_OK_START = 200
        const val HTTP_OK_END = 299
    }
}

@Singleton
internal class KtorPlayerHttpClient @Inject constructor(
    @PlayerResolveClient private val httpClient: HttpClient,
) : PlayerHttpClient {

    override suspend fun getText(
        url: String,
        headers: Map<String, String>,
        followRedirects: Boolean,
    ): PlayerHttpResponse = execute(url = url, method = HttpMethod.Get, headers = headers)

    override suspend fun postText(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): PlayerHttpResponse = execute(
        url = url,
        method = HttpMethod.Post,
        headers = headers,
        body = body,
        connectTimeoutMs = POST_CONNECT_TIMEOUT_MS,
        requestTimeoutMs = POST_REQUEST_TIMEOUT_MS,
    )

    override suspend fun head(
        url: String,
        headers: Map<String, String>,
    ): PlayerHttpResponse = execute(url = url, method = HttpMethod.Head, headers = headers)

    private suspend fun execute(
        url: String,
        method: HttpMethod,
        headers: Map<String, String>,
        body: String? = null,
        connectTimeoutMs: Long = GET_CONNECT_TIMEOUT_MS,
        requestTimeoutMs: Long = GET_REQUEST_TIMEOUT_MS,
    ): PlayerHttpResponse {
        val response: HttpResponse = httpClient.request(url) {
            this.method = method
            expectSuccess = false
            headers.forEach { (key, value) -> header(key, value) }
            if (body != null) setBody(body)
            timeout {
                connectTimeoutMillis = connectTimeoutMs
                requestTimeoutMillis = requestTimeoutMs
            }
        }

        val bytes = if (method == HttpMethod.Head) byteArrayOf() else response.bodyAsBytes()
        return PlayerHttpResponse(
            statusCode = response.status.value,
            body = bytes.toString(Charsets.UTF_8),
            headers = response.headers.toMap(),
            bodyBytes = bytes,
        )
    }

    private companion object {
        const val GET_CONNECT_TIMEOUT_MS = 10_000L
        const val GET_REQUEST_TIMEOUT_MS = 15_000L
        const val POST_CONNECT_TIMEOUT_MS = 8_000L
        const val POST_REQUEST_TIMEOUT_MS = 10_000L
    }
}
