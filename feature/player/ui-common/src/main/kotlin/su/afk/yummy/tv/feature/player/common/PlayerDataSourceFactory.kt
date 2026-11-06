package su.afk.yummy.tv.feature.player.common

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import su.afk.yummy.tv.core.common.player.AllohaEdgeHash
import su.afk.yummy.tv.core.network.di.StreamingOkHttp
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HTTP-источник плеера на OkHttp: в отличие от HttpURLConnection даёт HTTP/2 и переиспользует
 * TLS-соединения к CDN между сегментами и сериями. Клиент производный от общего — пул соединений
 * общий с остальным приложением. Таймауты и отдельный диспетчер задаёт [StreamingOkHttp]: OkHttpDataSource ходит
 * через enqueue, и общий лимит в 64 запроса не должен делиться с пачками загрузок картинок.
 */
@Singleton
class PlayerDataSourceFactory @Inject constructor(
    @StreamingOkHttp okHttpClient: OkHttpClient,
) {
    private val playerClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .addInterceptor { chain ->
                // Alloha's edge hash rotates during playback; only requests that carry it are touched.
                val fresh = AllohaEdgeHash.current
                val request = chain.request()
                val alloha = request.header(AllohaEdgeHash.HEADER) != null
                val response = chain.proceed(
                    if (fresh != null && alloha) {
                        request.newBuilder().header(AllohaEdgeHash.HEADER, fresh).build()
                    } else {
                        request
                    },
                )
                if (alloha && response.isSuccessful) {
                    response.body.contentLength().takeIf { it > 0 }?.let(AllohaEdgeHash.bytesLoaded::addAndGet)
                }
                response
            }
            .build()
    }

    fun create(headers: Map<String, String>): DataSource.Factory = okHttpFactory(headers)

    private fun okHttpFactory(headers: Map<String, String>): DataSource.Factory =
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
    }
}
