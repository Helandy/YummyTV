package su.afk.yummy.tv.data.player.extractor

import io.mockk.every
import io.mockk.mockkStatic
import su.afk.yummy.tv.data.player.network.PlayerHttpResponse
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import java.nio.charset.Charset

/**
 * Общие помощники тестов экстракторов.
 *
 * `org.json` берётся из org.json:json (`testImplementation`): в `android.jar` для unit-тестов `JSONObject`
 * пустой, и экстракторы молча уходили бы в `Failed`.
 */

internal fun httpResponse(
    status: Int = 200,
    body: String = "",
    headers: Map<String, List<String>> = emptyMap(),
): PlayerHttpResponse = PlayerHttpResponse(status, body, headers)

/** Ответ с телом в нужной кодировке: `body` декодируется как UTF-8, `bodyBytes` хранит оригинал. */
internal fun httpResponseBytes(
    status: Int = 200,
    text: String,
    charset: Charset,
    headers: Map<String, List<String>> = emptyMap(),
): PlayerHttpResponse {
    val bytes = text.toByteArray(charset)
    return PlayerHttpResponse(status, String(bytes, Charsets.UTF_8), headers, bytes)
}

internal fun streamRequest(
    iframeUrl: String,
    autoQualityLabel: String = "auto",
    forceRefresh: Boolean = false,
): PlayerStreamRequest = PlayerStreamRequest(
    iframeUrl = iframeUrl,
    autoQualityLabel = autoQualityLabel,
    forceRefresh = forceRefresh,
)

/**
 * `android.util.Base64` в JVM-тестах не работает (заглушка бросает «not mocked»), а экстракторы
 * глотают исключение и уходят в «не удалось декодировать». Подменяем на `java.util.Base64`.
 * Снимается `BaseUnitTest.unmockkAll()`.
 */
internal fun stubAndroidBase64() {
    mockkStatic(android.util.Base64::class)
    every { android.util.Base64.decode(any<String>(), any()) } answers {
        java.util.Base64.getMimeDecoder().decode(firstArg<String>())
    }
    every { android.util.Base64.decode(any<ByteArray>(), any()) } answers {
        java.util.Base64.getMimeDecoder().decode(firstArg<ByteArray>())
    }
    every { android.util.Base64.encodeToString(any<ByteArray>(), any()) } answers {
        java.util.Base64.getEncoder().encodeToString(firstArg<ByteArray>())
    }
}

/**
 * Маршрутизирующий HTTP-клиент: ответ выбирается по методу и подстроке URL, все вызовы пишутся в
 * [calls]. Удобнее мока, когда экстрактор делает несколько разных запросов подряд.
 * Позже заданный маршрут перекрывает ранее заданный с тем же совпадением.
 */
internal class FakePlayerHttpClient : su.afk.yummy.tv.data.player.network.PlayerHttpClient {

    data class Call(
        val method: String,
        val url: String,
        val headers: Map<String, String>,
        val body: String? = null,
    )

    private class Route(
        val method: String,
        val urlPart: String,
        val respond: (Call) -> PlayerHttpResponse,
    )

    val calls = mutableListOf<Call>()
    private val routes = mutableListOf<Route>()

    fun get(urlPart: String, response: PlayerHttpResponse) = get(urlPart) { response }

    fun get(urlPart: String, respond: (Call) -> PlayerHttpResponse) {
        routes.add(0, Route("GET", urlPart, respond))
    }

    fun post(urlPart: String, response: PlayerHttpResponse) {
        routes.add(0, Route("POST", urlPart) { response })
    }

    fun head(urlPart: String, response: PlayerHttpResponse) {
        routes.add(0, Route("HEAD", urlPart) { response })
    }

    /** Запросы выбранного метода, чей URL содержит [urlPart]. */
    fun callsTo(method: String, urlPart: String = ""): List<Call> =
        calls.filter { it.method == method && it.url.contains(urlPart) }

    override suspend fun getText(
        url: String,
        headers: Map<String, String>,
        followRedirects: Boolean,
    ): PlayerHttpResponse = handle(Call("GET", url, headers))

    override suspend fun postText(
        url: String,
        body: String,
        headers: Map<String, String>,
    ): PlayerHttpResponse = handle(Call("POST", url, headers, body))

    override suspend fun head(
        url: String,
        headers: Map<String, String>,
    ): PlayerHttpResponse = handle(Call("HEAD", url, headers))

    private fun handle(call: Call): PlayerHttpResponse {
        calls += call
        val route = routes.firstOrNull { it.method == call.method && call.url.contains(it.urlPart) }
            ?: throw IllegalStateException("No route for ${call.method} ${call.url}")
        return route.respond(call)
    }
}
