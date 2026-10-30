package su.afk.yummy.tv.core.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import su.afk.yummy.tv.core.network.dns.tmdbImageDns
import su.afk.yummy.tv.core.network.BuildConfig
import su.afk.yummy.tv.core.network.yani.SensitiveDataMaskingLogger
import su.afk.yummy.tv.core.network.yani.YaniApiJson
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = YaniApiJson

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().dns(tmdbImageDns()).build()

    @Provides
    @Singleton
    fun provideHttpClient(okHttpClient: OkHttpClient): HttpClient = HttpClient(OkHttp) {
        engine { preconfigured = okHttpClient }
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
        }
        // Нагружено: экстракторы полагаются на этот плагин. Он добавляет Accept-Encoding и
        // декодирует ответ, а часть страниц плееров (Zedfilm) без gzip отвечает 404. Убрать или
        // заменить клиент без него нельзя, не проверив ZedfilmExtractor.
        install(ContentEncoding) {
            gzip()
            deflate()
        }
    }

    /**
     * Отдельный клиент, а не ContentNegotiation поверх общего: общий ходит в плееры и за
     * m3u8/HTML, и навязанный `Accept: application/json` менял бы ответы этих запросов.
     */
    @Provides
    @Singleton
    @UnauthenticatedJsonClient
    fun provideUnauthenticatedJsonClient(okHttpClient: OkHttpClient, json: Json): HttpClient =
        HttpClient(OkHttp) {
            engine { preconfigured = okHttpClient }
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
            }
            install(ContentNegotiation) { json(json) }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
            if (BuildConfig.DEBUG) {
                install(Logging) {
                    logger = SensitiveDataMaskingLogger(Logger.ANDROID)
                    level = LogLevel.BODY
                    // Клиент ещё качает APK с GitHub: тело файла в лог не нужно.
                    filter { request -> !request.url.host.endsWith("github.com") && !request.url.host.endsWith("githubusercontent.com") }
                    sanitizeHeader { header ->
                        header.equals(HttpHeaders.Authorization, ignoreCase = true) ||
                            header.startsWith("X-Sig", ignoreCase = true)
                    }
                }
            }
        }

    private const val CONNECT_TIMEOUT_MS = 10_000L
    private const val REQUEST_TIMEOUT_MS = 15_000L
}
