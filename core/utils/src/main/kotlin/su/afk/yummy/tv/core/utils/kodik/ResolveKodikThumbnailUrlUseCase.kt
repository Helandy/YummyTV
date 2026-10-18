package su.afk.yummy.tv.core.utils.kodik

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.utils.coroutines.di.IoApplicationScope
import su.afk.yummy.tv.core.utils.kodik.di.KodikHttpClient
import su.afk.yummy.tv.core.utils.network.BrowserUserAgentProvider
import su.afk.yummy.tv.core.utils.network.RU_ACCEPT_LANGUAGE
import su.afk.yummy.tv.core.utils.network.isLikelyImageUrl
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Резолвит прямой URL превью серии из Kodik iframe.
 *
 * Один и тот же iframe-урл резолвится один раз за время жизни процесса: найденный URL кэшируется
 * в памяти. Неудача (`null`) помнится только [FAILURE_TTL_MS] — иначе один таймаут Kodik на старте
 * оставлял серию без превью до перезапуска. Параллельные запросы на один и тот же урл (например одна серия
 * одновременно отрисовывается в "продолжить просмотр" и в сетке серий) не плодят несколько HTML
 * запросов, а ждут один и тот же результат — single-flight на [IoApplicationScope], поэтому отмена
 * одного из ожидающих (например, экран закрылся) не тащит за собой отмену запроса для остальных.
 *
 * Прямая ссылка на картинку (серверная история отдаёт скриншоты с `i.kodikres.com`) возвращается
 * как есть: HTML из неё не достать.
 */
@Singleton
class ResolveKodikThumbnailUrlUseCase @Inject constructor(
    @KodikHttpClient private val httpClient: HttpClient,
    @IoApplicationScope private val scope: CoroutineScope,
    private val userAgents: BrowserUserAgentProvider,
    private val clock: AppClock,
) {

    // ConcurrentHashMap не допускает null-значений, поэтому результат (в т.ч. неудачный) заворачиваем.
    private val resolvedCache = ConcurrentHashMap<String, CacheEntry>()
    private val inFlight = ConcurrentHashMap<String, Deferred<String?>>()

    suspend operator fun invoke(iframeUrl: String): String? {
        val normalizedUrl = normalizeIframeUrl(iframeUrl)
        if (normalizedUrl.isLikelyImageUrl()) return toHttpsUrl(normalizedUrl)
        resolvedCache[normalizedUrl]?.takeIf { it.isFresh() }?.let { return it.url }

        val deferred = inFlight.computeIfAbsent(normalizedUrl) {
            scope.async { resolve(normalizedUrl) }
        }
        try {
            val result = deferred.await()
            resolvedCache[normalizedUrl] = CacheEntry(result, clock.nowMillis())
            return result
        } finally {
            inFlight.remove(normalizedUrl, deferred)
        }
    }

    private class CacheEntry(val url: String?, val resolvedAtMs: Long)

    private fun CacheEntry.isFresh(): Boolean =
        url != null || clock.nowMillis() - resolvedAtMs < FAILURE_TTL_MS

    private suspend fun resolve(normalizedUrl: String): String? {
        return runSuspendCatching {
            val html = fetchHtml(normalizedUrl)
            parsePosterUrl(html)?.let(::toHttpsUrl)
        }.getOrNull()
    }

    private suspend fun fetchHtml(url: String): String =
        httpClient.get(url) {
            header("Referer", "https://yani.tv/")
            header("User-Agent", userAgents.userAgent)
            header("Accept-Language", RU_ACCEPT_LANGUAGE)
            timeout {
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                requestTimeoutMillis = READ_TIMEOUT_MS
            }
        }.bodyAsText()

    private companion object {
        const val CONNECT_TIMEOUT_MS = 8_000L
        const val READ_TIMEOUT_MS = 10_000L
        const val FAILURE_TTL_MS = 60_000L
    }
}

fun normalizeIframeUrl(url: String): String = when {
    url.startsWith("//") -> "https:$url"
    url.startsWith("http") -> url
    else -> "https://$url"
}

private fun toHttpsUrl(url: String): String = when {
    url.startsWith("//") -> "https:$url"
    url.startsWith("http://") -> url.replaceFirst("http://", "https://")
    else -> url
}

private fun parsePosterUrl(html: String): String? =
    Regex("""['"]((https?:)?//[^'" ]*thumb001\.[a-z]+)['"]""")
        .find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
