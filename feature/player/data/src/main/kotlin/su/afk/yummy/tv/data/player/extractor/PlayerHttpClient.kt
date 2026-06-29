package su.afk.yummy.tv.data.player.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
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
internal class UrlConnectionPlayerHttpClient @Inject constructor() : PlayerHttpClient {

    override suspend fun getText(
        url: String,
        headers: Map<String, String>,
        followRedirects: Boolean,
    ): PlayerHttpResponse = withContext(Dispatchers.IO) {
        execute(url = url, method = "GET", headers = headers, followRedirects = followRedirects)
    }

    override suspend fun postText(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): PlayerHttpResponse = withContext(Dispatchers.IO) {
        execute(url = url, method = "POST", headers = headers, body = body)
    }

    override suspend fun head(
        url: String,
        headers: Map<String, String>,
    ): PlayerHttpResponse = withContext(Dispatchers.IO) {
        execute(url = url, method = "HEAD", headers = headers)
    }

    private fun execute(
        url: String,
        method: String,
        headers: Map<String, String>,
        followRedirects: Boolean = true,
        body: String? = null,
    ): PlayerHttpResponse {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = method
            conn.connectTimeout = if (method == "POST") 8_000 else 10_000
            conn.readTimeout = if (method == "POST") 10_000 else 15_000
            conn.instanceFollowRedirects = followRedirects
            headers.forEach { (key, value) -> conn.setRequestProperty(key, value) }
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }

            val statusCode = conn.responseCode
            val stream = when {
                method == "HEAD" -> null
                statusCode in 200..299 -> conn.inputStream
                else -> conn.errorStream ?: runCatching { conn.inputStream }.getOrNull()
            }
            val bytes = stream?.use { it.readBytes() } ?: byteArrayOf()
            PlayerHttpResponse(
                statusCode = statusCode,
                body = bytes.toString(Charsets.UTF_8),
                headers = conn.headerFields
                    .filterKeys { it != null }
                    .mapKeys { it.key.orEmpty() },
                bodyBytes = bytes,
            )
        } finally {
            conn.disconnect()
        }
    }
}
