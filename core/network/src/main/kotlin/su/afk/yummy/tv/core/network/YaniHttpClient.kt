package su.afk.yummy.tv.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.SettingsStore

const val YANI_BASE_URL = "https://api.yani.tv"
private const val YANI_API_HOST = "api.yani.tv"
private const val YANI_APPLICATION_HEADER = "X-Application"
private const val YANI_AUTHORIZATION_PREFIX = "Bearer "

fun buildYaniHttpClient(
    settingsStore: SettingsStore,
    yaniAuthPreferences: YaniAuthPreferences,
): HttpClient {
    val headerCache = YaniRequestHeaderCache(settingsStore, yaniAuthPreferences)

    return HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 20_000
            socketTimeoutMillis = 20_000
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                },
            )
        }
        install(createClientPlugin("YaniApplicationHeader") {
            onRequest { request, _ ->
                if (request.url.host == YANI_API_HOST) {
                    val (token, refreshToken) = headerCache.current()
                    if (token.isNotBlank()) {
                        request.headers.remove(YANI_APPLICATION_HEADER)
                        request.headers.append(YANI_APPLICATION_HEADER, token)
                    }
                    if (refreshToken.isNotBlank()) {
                        request.headers.remove(HttpHeaders.Authorization)
                        request.headers.append(HttpHeaders.Authorization, YANI_AUTHORIZATION_PREFIX + refreshToken)
                    }
                }
            }
        })
        if (BuildConfig.DEBUG) {
            install(Logging) {
                logger = Logger.ANDROID
                level = LogLevel.HEADERS
                sanitizeHeader { header ->
                    header.equals(HttpHeaders.Authorization, ignoreCase = true) ||
                        header.equals(HttpHeaders.Cookie, ignoreCase = true) ||
                        header.equals(HttpHeaders.SetCookie, ignoreCase = true) ||
                        header.equals(YANI_APPLICATION_HEADER, ignoreCase = true)
                }
            }
        }
    }
}

private class YaniRequestHeaderCache(
    private val settingsStore: SettingsStore,
    private val yaniAuthPreferences: YaniAuthPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initialLoadMutex = Mutex()

    @Volatile private var loaded = false
    @Volatile private var applicationToken = ""
    @Volatile private var refreshToken = ""

    init {
        scope.launch {
            settingsStore.yaniApplicationToken.collectLatest { token ->
                applicationToken = token
            }
        }
        scope.launch {
            yaniAuthPreferences.refreshToken.collectLatest { token ->
                refreshToken = token
            }
        }
    }

    suspend fun current(): Pair<String, String> {
        if (!loaded) loadInitialValues()
        return applicationToken to refreshToken
    }

    private suspend fun loadInitialValues() {
        initialLoadMutex.withLock {
            if (loaded) return
            coroutineScope {
                val applicationTokenDeferred = async { settingsStore.yaniApplicationToken.first() }
                val refreshTokenDeferred = async { yaniAuthPreferences.refreshToken.first() }
                applicationToken = applicationTokenDeferred.await()
                refreshToken = refreshTokenDeferred.await()
            }
            loaded = true
        }
    }
}
