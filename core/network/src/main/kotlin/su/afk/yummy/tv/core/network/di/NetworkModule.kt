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
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
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
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient()

    @Provides
    @Singleton
    fun provideHttpClient(okHttpClient: OkHttpClient): HttpClient = HttpClient(OkHttp) {
        engine { preconfigured = okHttpClient }
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
        }
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
        }

    private const val CONNECT_TIMEOUT_MS = 15_000L
    private const val REQUEST_TIMEOUT_MS = 20_000L
}
