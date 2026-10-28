package su.afk.yummy.tv.data.details.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import su.afk.yummy.tv.core.network.di.UnauthenticatedJsonClient
import su.afk.yummy.tv.data.details.dto.MalEpisodesDto
import java.io.IOException
import javax.inject.Inject

/**
 * Любой API со схемой Jikan v4. Публичный Jikan закрыт 1 октября 2026, поэтому используем Tenrai: те же ответы,
 * без ключа до 4 запросов в секунду и 120 в минуту.
 */
private const val BASE_URL = "https://api.tenrai.org/v1"
private const val MAX_PAGES = 30
private const val MAX_PARALLEL_PAGES = 3

/** Старты запросов расходятся не чаще, чем раз в 260 мс (~3,8 в секунду), на все тайтлы сразу. */
private const val MIN_REQUEST_SPACING_MS = 260L

/** Серии тайтла из MyAnimeList: пометки филлер/рекап и картинки серий. */
class MalEpisodeFlagsApi @Inject constructor(
    @param:UnauthenticatedJsonClient private val client: HttpClient,
) {
    private val gateMutex = Mutex()
    private var lastRequestStartMs = 0L

    // Общий для всех тайтлов предел одновременных запросов: параллельные страницы не должны сложиться между тайтлами.
    private val inFlight = Semaphore(MAX_PARALLEL_PAGES)

    /**
     * Все серии тайтла, склеенные из страниц. 404 — «серий нет». Первая страница нужна, чтобы узнать их число; остальные
     * качаются параллельно, но не быстрее лимита API. [onFirstPage] получает серии первой страницы, пока грузятся
     * остальные (только если они есть). Ошибка любой страницы (в том числе 429) пробрасывается: неполный список
     * в кэш попасть не должен.
     */
    suspend fun getEpisodes(
        malId: Int,
        onFirstPage: suspend (MalEpisodesDto) -> Unit = {},
    ): MalEpisodesDto {
        val first = fetchPage(malId, 1) ?: return MalEpisodesDto()
        val episodes = first.data.toMutableList()
        if (!first.pagination.hasNextPage) return MalEpisodesDto(data = episodes)
        onFirstPage(MalEpisodesDto(data = first.data))

        val lastKnownPage = first.pagination.lastVisiblePage.coerceIn(2, MAX_PAGES)
        val rest = coroutineScope {
            (2..lastKnownPage).map { page -> async { fetchPage(malId, page) } }.awaitAll()
        }
        rest.forEach { page -> page?.let { episodes += it.data } }

        // last_visible_page бывает занижен (у длинных тайтлов): доходим до конца по hasNextPage.
        var hasNext = rest.lastOrNull()?.pagination?.hasNextPage == true
        var page = lastKnownPage + 1
        while (hasNext && page <= MAX_PAGES) {
            val body = fetchPage(malId, page) ?: break
            episodes += body.data
            hasNext = body.pagination.hasNextPage
            page++
        }
        return MalEpisodesDto(data = episodes)
    }

    private suspend fun fetchPage(malId: Int, page: Int): MalEpisodesDto? = inFlight.withPermit {
        awaitRequestSlot()
        val response = client.get("$BASE_URL/anime/$malId/episodes") {
            parameter("page", page)
            expectSuccess = false
        }
        if (response.status == HttpStatusCode.NotFound) return@withPermit null
        if (!response.status.isSuccess()) throw IOException("Episodes API: HTTP ${response.status.value}")
        response.body<MalEpisodesDto>()
    }

    private suspend fun awaitRequestSlot() {
        gateMutex.withLock {
            val wait = lastRequestStartMs + MIN_REQUEST_SPACING_MS - System.currentTimeMillis()
            if (wait > 0) delay(wait)
            lastRequestStartMs = System.currentTimeMillis()
        }
    }
}
