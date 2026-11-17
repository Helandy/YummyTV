package su.afk.yummy.tv.core.network.yani

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import su.afk.yummy.tv.core.network.yummy.SiteApiConfig
import su.afk.yummy.tv.core.network.yummy.SiteRequestSigner
import su.afk.yummy.tv.core.network.yummy.signSiteRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Запасной путь для каталога, деталей и видео, когда api.yani.tv недоступен: те же данные из собственной копии на сервере
 * YummyTV (`{apiBaseUrl}/yani/...`, формат ответа как у yani, запросы подписываются как у остальных ручек сервера).
 *
 * [call] сначала идёт в yani. Сетевой сбой, таймаут, 5xx или нечитаемый ответ считаются недоступностью yani: запрос повторяется
 * по копии, а при ошибке копии пробрасывается исходная ошибка yani, чтобы вызывающий код (кэш, «устаревшие» данные) работал как
 * раньше. Ответ 4xx от yani — штатный («нет такого тайтла»), он в копию не уходит.
 *
 * После [FAILURES_TO_OPEN] сбоев yani подряд на [OPEN_MS] открывается «автомат»: запросы сразу идут в копию, чтобы не ждать
 * таймаут каждый раз; yani при этом пробуется заново, только если копия ответить не смогла.
 */
@Singleton
class YaniMirrorFallback @Inject constructor(
    private val clientProvider: YaniHttpClientProvider,
    private val siteApiConfig: SiteApiConfig,
    private val signers: Set<@JvmSuppressWildcards SiteRequestSigner>,
) {
    internal var now: () -> Long = System::currentTimeMillis

    private val _state = MutableStateFlow(MirrorState())

    /** Идёт ли чтение из копии вместо yani и насколько свежа копия: по нему UI показывает плашку. */
    val state: StateFlow<MirrorState> = _state.asStateFlow()

    @Volatile
    private var lastCopyUpdatedSec: Long? = null

    @Volatile
    private var failures = 0

    @Volatile
    private var openUntil = 0L

    /**
     * GET к копии: [path] относительно `{apiBaseUrl}`, например `/yani/anime/123`. Статус не 2xx — исключение Ktor.
     * Если основной адрес не ответил (сеть, 5xx), пробуется запасной ([SiteApiConfig.fallbackBaseUrl]); 4xx — это ответ копии,
     * второй адрес не спрашивается.
     */
    suspend fun mirrorGet(path: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        val fallbackBase = siteApiConfig.fallbackBaseUrl
        val response = try {
            get(siteApiConfig.baseUrl + path, block)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is ClientRequestException || fallbackBase.isEmpty()) throw e
            get(fallbackBase + path, block)
        }
        lastCopyUpdatedSec = response.headers[UPDATED_HEADER]?.toLongOrNull()
        return response
    }

    private suspend fun get(url: String, block: HttpRequestBuilder.() -> Unit): HttpResponse =
        clientProvider.get().get(url) {
            block()
            signSiteRequest(signers, "GET", url)
        }

    /** Выполняет [primary] (yani), а при его недоступности — [mirror] (копия). */
    suspend fun <T> call(primary: suspend () -> T, mirror: suspend () -> T): T {
        if (now() < openUntil) {
            return try {
                mirror().also(::markCopy)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                primaryTracked(primary)
            }
        }
        return try {
            primaryTracked(primary)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!e.isYaniOutage()) throw e
            try {
                mirror().also(::markCopy)
            } catch (m: CancellationException) {
                throw m
            } catch (_: Exception) {
                throw e
            }
        }
    }

    /** [primary] с учётом счётчика сбоев; исключение пробрасывается, при недоступности yani счётчик растёт. */
    private suspend fun <T> primaryTracked(primary: suspend () -> T): T = try {
        primary().also {
            failures = 0
            openUntil = 0L
            _state.value = MirrorState()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (e.isYaniOutage() && ++failures >= FAILURES_TO_OPEN) openUntil = now() + OPEN_MS
        throw e
    }

    private fun markCopy(@Suppress("UNUSED_PARAMETER") result: Any?) {
        _state.value = MirrorState(active = true, copyUpdatedAtSec = lastCopyUpdatedSec)
    }

    private fun Exception.isYaniOutage() = this !is ClientRequestException

    internal companion object {
        const val UPDATED_HEADER = "X-Mirror-Updated"
        const val FAILURES_TO_OPEN = 2
        const val OPEN_MS = 5 * 60 * 1000L
    }
}

/** [active] — ответы сейчас идут из копии YummyTV, а не из yani; [copyUpdatedAtSec] — когда копия прочитана у yani (эпоха, секунды), если известно. */
data class MirrorState(val active: Boolean = false, val copyUpdatedAtSec: Long? = null)
