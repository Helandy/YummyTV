package su.afk.yummy.tv.core.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import su.afk.yummy.tv.core.network.NetworkTimeouts
import su.afk.yummy.tv.core.network.okHttpKtorClient
import su.afk.yummy.tv.core.utils.kodik.di.KodikHttpClient
import su.afk.yummy.tv.core.network.applyTimeouts
import su.afk.yummy.tv.core.network.BuildConfig
import su.afk.yummy.tv.core.network.dns.tmdbImageDns
import su.afk.yummy.tv.core.network.dns.imageHostTimeouts
import su.afk.yummy.tv.core.network.progress.ResolveProgressPlugin
import su.afk.yummy.tv.core.network.yani.SensitiveDataMaskingLogger
import su.afk.yummy.tv.core.network.yani.YaniApiJson
import su.afk.yummy.tv.core.utils.logging.AppLogRecordingSettings
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = YaniApiJson

    /**
     * Базовый клиент: источник пула соединений, диспетчера и DNS для всех остальных. Не нейтральный: DNS
     * обходит блокировку картинок TMDB (DoH) и интерцептор даёт короткие таймауты хостам картинок. Остальные клиенты
     * приложения (Ktor, [StreamingOkHttp], [DownloadOkHttp], [AllohaOkHttp]) строятся от него через `newBuilder()`.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder().dns(tmdbImageDns()).addInterceptor(imageHostTimeouts(BuildConfig.STATIC_HOST)).build()

    @Provides
    @Singleton
    @StreamingOkHttp
    fun provideStreamingOkHttp(okHttpClient: OkHttpClient): OkHttpClient =
        okHttpClient.newBuilder().dispatcher(Dispatcher()).applyTimeouts(NetworkTimeouts.Streaming).build()

    @Provides
    @Singleton
    @DownloadOkHttp
    fun provideDownloadOkHttp(okHttpClient: OkHttpClient): OkHttpClient =
        okHttpClient.newBuilder().dispatcher(Dispatcher()).applyTimeouts(NetworkTimeouts.Download).build()

    @Provides
    @Singleton
    @AllohaOkHttp
    fun provideAllohaOkHttp(okHttpClient: OkHttpClient): OkHttpClient =
        okHttpClient.newBuilder().dispatcher(Dispatcher()).applyTimeouts(NetworkTimeouts.Alloha).build()

    /**
     * Клиент резолва потоков плееров. Отдельный, чтобы `ResolveProgressPlugin` не ловил запросы
     * остальных частей приложения; пул соединений общий с остальными через [okHttpClient].
     */
    @Provides
    @Singleton
    @PlayerResolveClient
    fun providePlayerResolveClient(okHttpClient: OkHttpClient): HttpClient =
        // Нагружено: Zedfilm без gzip отвечает 404, плагин добавляет Accept-Encoding и декодирует ответ.
        okHttpKtorClient(okHttpClient, timeouts = NetworkTimeouts.Api) {
            install(ResolveProgressPlugin)
        }

    /**
     * Отдельный клиент, а не ContentNegotiation поверх общего: общий ходит в плееры и за
     * m3u8/HTML, и навязанный `Accept: application/json` менял бы ответы этих запросов.
     */
    @Provides
    @Singleton
    @UnauthenticatedJsonClient
    fun provideUnauthenticatedJsonClient(
        okHttpClient: OkHttpClient,
        json: Json,
        logRecordingSettings: AppLogRecordingSettings,
    ): HttpClient =
        okHttpKtorClient(okHttpClient, timeouts = NetworkTimeouts.Api) {
            install(ContentNegotiation) { json(json) }
            // В release тело пишется, только пока пользователь включил запись логов: filter считается
            // на каждый вызов, при false Ktor тело не читает.
            install(Logging) {
                logger = SensitiveDataMaskingLogger(Logger.ANDROID)
                level = LogLevel.BODY
                // Клиент ещё качает APK с GitHub: тело файла в лог не нужно.
                filter { request ->
                    (BuildConfig.DEBUG || logRecordingSettings.isEnabled) &&
                        !request.url.host.endsWith("github.com") &&
                        !request.url.host.endsWith("githubusercontent.com")
                }
                sanitizeHeader { header ->
                    header.equals(HttpHeaders.Authorization, ignoreCase = true) ||
                        header.startsWith("X-Sig", ignoreCase = true)
                }
            }
        }

    /**
     * Ktor-клиент превью серий Kodik. Общий OkHttpClient приложения: пул соединений и TLS-сессии к Kodik
     * переиспользуются вместо отдельного движка со своими пулом и потоками. Таймауты запросы задают сами.
     */
    @Provides
    @Singleton
    @KodikHttpClient
    fun provideKodikHttpClient(okHttpClient: OkHttpClient): HttpClient =
        okHttpKtorClient(okHttpClient, allowRequestTimeouts = true)
}
