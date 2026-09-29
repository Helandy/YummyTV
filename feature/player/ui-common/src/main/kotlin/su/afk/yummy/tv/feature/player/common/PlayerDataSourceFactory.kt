package su.afk.yummy.tv.feature.player.common

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HTTP-источник плеера на OkHttp: в отличие от HttpURLConnection даёт HTTP/2 и переиспользует
 * TLS-соединения к CDN между сегментами и сериями. Клиент производный от общего — пул соединений
 * общий с остальным приложением, таймауты свои. Диспетчер отдельный: OkHttpDataSource ходит через
 * enqueue, и общий лимит в 64 запроса не должен делиться с пачками загрузок картинок.
 */
@Singleton
class PlayerDataSourceFactory @Inject constructor(
    okHttpClient: OkHttpClient,
) {
    private val playerClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .dispatcher(Dispatcher())
            .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build()
    }

    fun create(headers: Map<String, String>): DataSource.Factory =
        OkHttpDataSource.Factory(playerClient).apply {
            headers.userAgent()?.takeIf { it.isNotBlank() }?.let(::setUserAgent)
            val requestHeaders =
                headers.filterKeys { !it.equals(USER_AGENT_HEADER, ignoreCase = true) }
            if (requestHeaders.isNotEmpty()) setDefaultRequestProperties(requestHeaders)
        }

    private fun Map<String, String>.userAgent(): String? =
        entries.firstOrNull { (key, _) -> key.equals(USER_AGENT_HEADER, ignoreCase = true) }?.value

    private companion object {
        const val USER_AGENT_HEADER = "User-Agent"

        // 2x от дефолтов HTTP-источника Media3 (8с): меньше ложных
        // ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT на медленных сетях/CDN.
        const val CONNECT_TIMEOUT_MS = 16_000L
        const val READ_TIMEOUT_MS = 16_000L
    }
}
