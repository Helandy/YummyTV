package su.afk.yummy.tv.source.anilibria.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import su.afk.yummy.tv.core.model.anime.utils.animeTitleKey
import su.afk.yummy.tv.core.model.anime.utils.prioritizeAnimeTitles
import su.afk.yummy.tv.core.model.anime.utils.stripSeasonSuffix
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaLookupRequestDto
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaSearchItemDto
import su.afk.yummy.tv.source.anilibria.data.mapper.AniLibriaMapper
import su.afk.yummy.tv.source.anilibria.data.network.AniLibriaApi
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaRelease
import su.afk.yummy.tv.source.anilibria.domain.repository.AniLibriaRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
internal class AniLibriaRepositoryImpl @Inject constructor(
    private val api: AniLibriaApi,
) : AniLibriaRepository {

    private data class Cached(val release: AniLibriaRelease?, val fetchedAtMs: Long)

    private data class CachedList(val releases: List<AniLibriaRelease>, val fetchedAtMs: Long)

    private data class CachedSearch(val items: List<AniLibriaSearchItemDto>, val fetchedAtMs: Long)

    private val byMalId = ConcurrentHashMap<Int, CachedList>()
    private val byReleaseId = ConcurrentHashMap<Int, Cached>()
    private val bySearchQuery = ConcurrentHashMap<String, CachedSearch>()

    override suspend fun findReleases(malId: Int, titles: List<String>, year: Int?): List<AniLibriaRelease> {
        val now = System.currentTimeMillis()
        byMalId[malId]?.let { cached ->
            val ttl = if (cached.releases.isEmpty()) EMPTY_TTL_MS else TTL_MS
            if (now - cached.fetchedAtMs < ttl) return cached.releases
        }

        // Сбой сети в кэш не попадает: исключение уйдёт наверх, источник просто не появится в этот раз.
        val releases = coroutineScope {
            findReleaseIds(malId, titles, year).map { id -> async { getRelease(id) } }.awaitAll().filterNotNull()
        }
        byMalId[malId] = CachedList(releases, System.currentTimeMillis())
        return releases
    }

    override suspend fun getRelease(releaseId: Int): AniLibriaRelease? {
        val now = System.currentTimeMillis()
        byReleaseId[releaseId]?.takeIf { now - it.fetchedAtMs < TTL_MS }?.let { return it.release }

        val dto = withRetry { api.getRelease(releaseId) }
        // У части релизов MAL id не заполнен: они находятся по названию и году, поэтому id релиза здесь не обязателен.
        val release = dto?.let { AniLibriaMapper.toRelease(it.mal?.id ?: 0, it) }
        byReleaseId[releaseId] = Cached(release, System.currentTimeMillis())
        return release
    }

    private suspend fun findReleaseIds(malId: Int, titles: List<String>, year: Int?): List<Int> {
        val sorted = prioritizeAnimeTitles("", titles, MAX_TITLES)
        // Основной путь — копия каталога на сервере YummyTV: один запрос и ни одного поиска в API AniLibria. Ответ сервера
        // (в том числе «релизов нет») окончателен; к поиску по названию возвращаемся только когда сервер недоступен.
        val fromCatalog = try {
            sorted.firstOrNull()?.let { title ->
                api.findReleaseIds(AniLibriaLookupRequestDto(malId, title, sorted.drop(1), year))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (fromCatalog != null) return fromCatalog
        return searchReleaseIds(malId, sorted, year)
    }

    /** Запасной путь: поиск по названиям в самом API AniLibria. */
    private suspend fun searchReleaseIds(malId: Int, sorted: List<String>, year: Int?): List<Int> {
        val queries = sorted.flatMap { listOf(it, stripSeasonSuffix(it)) }
            .filter { it.isNotBlank() }
            .distinctBy(::animeTitleKey)
            .take(MAX_QUERIES)
        val wanted = sorted.map(::animeTitleKey).toSet()

        // Все запросы уходят одновременно, чтобы пользователь не ждал их по очереди. Ждём все: озвучка и новый дубляж
        // одного тайтла могут найтись разными запросами. Сбой одного запроса не отменяет остальные.
        val marked = LinkedHashSet<Int>()
        val unmarked = LinkedHashMap<Int, AniLibriaSearchItemDto>()
        var failure: Exception? = null
        val results = supervisorScope {
            queries.map { query ->
                async {
                    try {
                        Result.success(search(query))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }
            }.awaitAll()
        }
        for (result in results) {
            val found = result.getOrElse {
                failure = it as? Exception ?: return@getOrElse emptyList()
                emptyList()
            }
            found.filter { it.mal?.id == malId }.forEach { marked += it.id }
            // Релиз без MAL id принимаем только при точном названии и году: иначе легко взять соседний сезон.
            found.filter { it.mal?.id == null && it.matches(wanted, year) }.forEach { unmarked.putIfAbsent(it.id, it) }
        }
        // Часть запросов упала и ничего не нашлось: это сбой, а не отсутствие релиза — не кэшируем пустой ответ.
        if (marked.isEmpty() && unmarked.isEmpty() && failure != null) throw failure
        return if (marked.isNotEmpty()) marked.toList() else unmarked.keys.singleOrNull()?.let(::listOf).orEmpty()
    }

    private suspend fun search(query: String): List<AniLibriaSearchItemDto> {
        val key = animeTitleKey(query)
        val now = System.currentTimeMillis()
        bySearchQuery[key]?.takeIf { now - it.fetchedAtMs < TTL_MS }?.let { return it.items }
        val items = withRetry { api.search(query) }
        bySearchQuery[key] = CachedSearch(items, System.currentTimeMillis())
        return items
    }

    /** Один повтор после короткой паузы: API периодически отвечает 5xx/таймаутом, а следующая попытка проходит. */
    private suspend fun <T> withRetry(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        delay(RETRY_DELAY_MS)
        block()
    }

    private fun AniLibriaSearchItemDto.matches(wanted: Set<String>, year: Int?): Boolean {
        val names = listOfNotNull(name?.main, name?.english, name?.alternative).map(::animeTitleKey)
        if (names.none { it.isNotEmpty() && it in wanted }) return false
        val releaseYear = this.year
        return year == null || releaseYear == null || abs(year - releaseYear) <= 1
    }

    private companion object {
        const val TTL_MS = 30 * 60 * 1000L
        const val EMPTY_TTL_MS = 5 * 60 * 1000L
        const val RETRY_DELAY_MS = 400L
        const val MAX_QUERIES = 4
        const val MAX_TITLES = 30
    }
}
