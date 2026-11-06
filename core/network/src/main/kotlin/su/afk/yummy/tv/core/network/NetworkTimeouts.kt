package su.afk.yummy.tv.core.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Таймауты одного вида запросов, мс. [connectMs] и [requestMs] для OkHttp это таймаут соединения и чтения,
 * для Ktor `connectTimeoutMillis` и `requestTimeoutMillis`; [socketMs] есть только у Ktor-профилей, где его задают.
 */
data class HttpTimeouts(
    val connectMs: Long,
    val requestMs: Long,
    val socketMs: Long? = null,
)

/**
 * Все таймауты приложения в одном месте. Новый клиент берёт профиль отсюда, а не заводит свои константы.
 */
object NetworkTimeouts {
    /** Публичные сторонние API и общий JSON-клиент. */
    val Api = HttpTimeouts(connectMs = 10_000L, requestMs = 15_000L)

    /** Yani: GET с одним ретраем, поэтому худший случай около двух connect-таймаутов. */
    val Yani = HttpTimeouts(connectMs = 10_000L, requestMs = 20_000L, socketMs = 15_000L)

    /** Резолв потока плеера: страницы и m3u8 (GET, HEAD). */
    val ResolveGet = HttpTimeouts(connectMs = 10_000L, requestMs = 15_000L)

    /** Резолв потока плеера: POST (короче, чем GET: ответ нужен быстро или не нужен). */
    val ResolvePost = HttpTimeouts(connectMs = 8_000L, requestMs = 10_000L)

    /**
     * Сегменты и плейлисты при воспроизведении. В 2 раза больше дефолтов HTTP-источника Media3 (8 с):
     * меньше ложных `ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT` на медленных сетях и CDN.
     */
    val Streaming = HttpTimeouts(connectMs = 16_000L, requestMs = 16_000L)

    /** Загрузки серий. */
    val Download = HttpTimeouts(connectMs = 30_000L, requestMs = 30_000L)

    /** Alloha: страница плеера, `bnsi`, отчёты, WebSocket. */
    val Alloha = HttpTimeouts(connectMs = 10_000L, requestMs = 15_000L)

    /** Передача сессии по локальной сети: ТВ стоит в той же сети, долгого ожидания быть не должно. */
    val Lan = HttpTimeouts(connectMs = 5_000L, requestMs = 10_000L)

    /** DNS-over-HTTPS клиент. */
    const val DOH_SECONDS = 3

    /** Картинки с CDN, у которых есть запасные источники: упавший узел отваливается за секунды. */
    const val IMAGE_HOST_CONNECT_SECONDS = 3
    const val IMAGE_HOST_READ_SECONDS = 5
}

/** Применяет [timeouts] к OkHttp: соединение и чтение. */
fun OkHttpClient.Builder.applyTimeouts(timeouts: HttpTimeouts): OkHttpClient.Builder =
    connectTimeout(timeouts.connectMs, TimeUnit.MILLISECONDS)
        .readTimeout(timeouts.requestMs, TimeUnit.MILLISECONDS)
