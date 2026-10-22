package su.afk.yummy.tv.source.anilibria.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import su.afk.yummy.tv.core.model.anime.utils.animeTitleKey
import su.afk.yummy.tv.core.model.anime.utils.prioritizeAnimeTitles
import su.afk.yummy.tv.core.model.anime.utils.stripSeasonSuffix
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

    private val byMalId = ConcurrentHashMap<Int, Cached>()
    private val byReleaseId = ConcurrentHashMap<Int, Cached>()

    override suspend fun findRelease(malId: Int, titles: List<String>, year: Int?): AniLibriaRelease? {
        val now = System.currentTimeMillis()
        byMalId[malId]?.takeIf { now - it.fetchedAtMs < TTL_MS }?.let { return it.release }

        // Сбой сети в кэш не попадает: исключение уйдёт наверх, источник просто не появится в этот раз.
        val release = findReleaseId(malId, titles, year)?.let { id -> getRelease(id) }
        byMalId[malId] = Cached(release, now)
        return release
    }

    override suspend fun getRelease(releaseId: Int): AniLibriaRelease? {
        val now = System.currentTimeMillis()
        byReleaseId[releaseId]?.takeIf { now - it.fetchedAtMs < TTL_MS }?.let { return it.release }

        val dto = api.getRelease(releaseId)
        // У части релизов MAL id не заполнен: они находятся по названию и году, поэтому id релиза здесь не обязателен.
        val release = dto?.let { AniLibriaMapper.toRelease(it.mal?.id ?: 0, it) }
        byReleaseId[releaseId] = Cached(release, now)
        return release
    }

    private suspend fun findReleaseId(malId: Int, titles: List<String>, year: Int?): Int? {
        val sorted = prioritizeAnimeTitles("", titles, MAX_TITLES)
        val queries = sorted.flatMap { listOf(it, stripSeasonSuffix(it)) }.distinct().take(MAX_QUERIES)
        val wanted = sorted.map(::animeTitleKey).toSet()
        // Все запросы уходят одновременно, чтобы пользователь не ждал их по очереди; результаты разбираем в порядке приоритета.
        return coroutineScope {
            val searches = queries.map { query -> async { api.search(query) } }
            val unmarked = LinkedHashMap<Int, AniLibriaSearchItemDto>()
            for (search in searches) {
                val found = search.await()
                found.firstOrNull { it.mal?.id == malId }?.let { match ->
                    searches.forEach { it.cancel() }
                    return@coroutineScope match.id
                }
                // Релиз без MAL id принимаем только при точном названии и году: иначе легко взять соседний сезон.
                found.filter { it.mal?.id == null && it.matches(wanted, year) }.forEach { unmarked.putIfAbsent(it.id, it) }
            }
            unmarked.keys.singleOrNull()
        }
    }

    private fun AniLibriaSearchItemDto.matches(wanted: Set<String>, year: Int?): Boolean {
        val names = listOfNotNull(name?.main, name?.english, name?.alternative).map(::animeTitleKey)
        if (names.none { it.isNotEmpty() && it in wanted }) return false
        val releaseYear = this.year
        return year == null || releaseYear == null || abs(year - releaseYear) <= 1
    }

    private companion object {
        const val TTL_MS = 5 * 60 * 1000L
        const val MAX_QUERIES = 4
        const val MAX_TITLES = 30
    }
}
