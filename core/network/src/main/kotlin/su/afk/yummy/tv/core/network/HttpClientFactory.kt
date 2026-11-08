// Part of YummyTV — https://github.com/Helandy/YummyTV (CC BY-NC 4.0, see NOTICE)
package su.afk.yummy.tv.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import okhttp3.OkHttpClient

/**
 * Единственное место, где собирается Ktor-клиент на OkHttp. Общий каркас: движок поверх [base] (пул соединений,
 * диспетчер и DNS остаются общими), плагин таймаутов и распаковка gzip/deflate. Профиль клиента (JSON, логирование,
 * ретраи, свои плагины) добавляет [block].
 *
 * @param timeouts таймауты клиента по умолчанию; для каждого запроса их можно переопределить через `timeout { }`
 * @param allowRequestTimeouts поставить плагин таймаутов даже без [timeouts]: запросы клиента задают их сами
 * @param contentEncoding добавлять `Accept-Encoding` и распаковывать ответы; картинкам он не нужен
 */
fun okHttpKtorClient(
    base: OkHttpClient,
    timeouts: HttpTimeouts? = null,
    allowRequestTimeouts: Boolean = false,
    contentEncoding: Boolean = true,
    block: HttpClientConfig<OkHttpConfig>.() -> Unit = {},
): HttpClient = HttpClient(OkHttp) {
    engine { preconfigured = base }
    if (timeouts != null || allowRequestTimeouts) {
        install(HttpTimeout) {
            timeouts?.let {
                connectTimeoutMillis = it.connectMs
                requestTimeoutMillis = it.requestMs
                it.socketMs?.let { socket -> socketTimeoutMillis = socket }
            }
        }
    }
    if (contentEncoding) {
        install(ContentEncoding) {
            gzip()
            deflate()
        }
    }
    block()
}
