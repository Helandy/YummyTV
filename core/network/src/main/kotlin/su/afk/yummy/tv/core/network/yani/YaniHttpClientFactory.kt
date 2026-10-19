package su.afk.yummy.tv.core.network.yani

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.api.YaniAuthPreferences

private const val YANI_API_HOST = "api.yani.tv"
private const val YANI_APPLICATION_HEADER = "X-Application"
private const val YANI_LANGUAGE_HEADER = "Lang"
private const val YANI_AUTHORIZATION_PREFIX = "Bearer "

// connect- и socket-таймауты GET ретраятся один раз, поэтому худший случай ≈ 2 × connect.
private const val YANI_CONNECT_TIMEOUT_MS = 10_000L
private const val YANI_SOCKET_TIMEOUT_MS = 15_000L
private const val YANI_REQUEST_TIMEOUT_MS = 20_000L

fun buildYaniHttpClient(
    settingsStore: YaniAccountSettingsStore,
    yaniAuthPreferences: YaniAuthPreferences,
    okHttpClient: OkHttpClient,
    scope: CoroutineScope,
): HttpClient {
    val headerCache = YaniRequestHeaderCache(settingsStore, yaniAuthPreferences, scope)

    return HttpClient(OkHttp) {
        // Без этого 401/5xx с JSON-телом декодируются в «пустой успех» (DTO с дефолтами) и затирают
        // кэш. Запросы, которые сами разбирают статус/тело ошибки (логин, регистрация, сброс пароля,
        // YummyTV API), отключают проверку точечно: `expectSuccess = false`.
        expectSuccess = true
        engine {
            preconfigured = okHttpClient
        }
        install(HttpTimeout) {
            connectTimeoutMillis = YANI_CONNECT_TIMEOUT_MS
            requestTimeoutMillis = YANI_REQUEST_TIMEOUT_MS
            socketTimeoutMillis = YANI_SOCKET_TIMEOUT_MS
        }
        // Ретраим только GET: мутации могут быть неидемпотентны.
        install(HttpRequestRetry) {
            maxRetries = 1
            retryIf { request, response ->
                request.method == HttpMethod.Get && response.status.value in 500..599
            }
            retryOnExceptionIf { request, cause ->
                request.method == HttpMethod.Get &&
                    cause !is CancellationException &&
                    // 4xx и таймаут запроса повторять бессмысленно: ответ не изменится, а спиннер вырастет.
                    cause !is ClientRequestException &&
                    cause !is HttpRequestTimeoutException
            }
            delayMillis { retry -> 500L * retry }
        }
        install(ContentNegotiation) {
            json(YaniApiJson)
        }
        install(ContentEncoding) {
            gzip()
            deflate()
        }
        install(
            createClientPlugin("YaniApplicationHeader") {
                onRequest { request, _ ->
                    if (request.url.host == YANI_API_HOST) {
                        val headers = headerCache.current()
                        if (headers.applicationToken.isNotBlank()) {
                            request.headers.remove(YANI_APPLICATION_HEADER)
                            request.headers.append(YANI_APPLICATION_HEADER, headers.applicationToken)
                        }
                        if (headers.contentLanguageCode.isNotBlank()) {
                            request.headers.remove(YANI_LANGUAGE_HEADER)
                            request.headers.append(YANI_LANGUAGE_HEADER, headers.contentLanguageCode)
                        }
                        if (
                            headers.refreshToken.isNotBlank() &&
                            request.headers[HttpHeaders.Authorization].isNullOrBlank()
                        ) {
                            request.headers.remove(HttpHeaders.Authorization)
                            request.headers.append(
                                HttpHeaders.Authorization,
                                YANI_AUTHORIZATION_PREFIX + headers.refreshToken,
                            )
                        }
                    }
                }
            },
        )
        // Логи включены и в release: пользователи присылают экспортированные логи для разбора.
        // Секреты маскирует SensitiveDataMaskingLogger, заголовки авторизации скрыты sanitizeHeader.
        install(Logging) {
            logger = SensitiveDataMaskingLogger(Logger.ANDROID)
            level = LogLevel.BODY
            sanitizeHeader { header ->
                header.equals(HttpHeaders.Authorization, ignoreCase = true) ||
                    header.equals(HttpHeaders.Cookie, ignoreCase = true) ||
                    header.equals(HttpHeaders.SetCookie, ignoreCase = true) ||
                    header.equals(YANI_APPLICATION_HEADER, ignoreCase = true)
            }
        }
    }
}
