package su.afk.yummy.tv.source.anilibria.data.repository

import su.afk.yummy.tv.source.anilibria.data.mapper.AniLibriaMapper
import su.afk.yummy.tv.source.anilibria.data.network.AniLibriaApi
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaRelease
import su.afk.yummy.tv.source.anilibria.domain.repository.AniLibriaRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AniLibriaRepositoryImpl @Inject constructor(
    private val api: AniLibriaApi,
) : AniLibriaRepository {

    private data class Cached(val release: AniLibriaRelease?, val fetchedAtMs: Long)

    private val byMalId = ConcurrentHashMap<Int, Cached>()
    private val byReleaseId = ConcurrentHashMap<Int, Cached>()

    override suspend fun findRelease(malId: Int, titles: List<String>): AniLibriaRelease? {
        val now = System.currentTimeMillis()
        byMalId[malId]?.takeIf { now - it.fetchedAtMs < TTL_MS }?.let { return it.release }

        // Сбой сети в кэш не попадает: исключение уйдёт наверх, источник просто не появится в этот раз.
        val release = findReleaseId(malId, titles)?.let { id -> getRelease(id) }
        byMalId[malId] = Cached(release, now)
        return release
    }

    override suspend fun getRelease(releaseId: Int): AniLibriaRelease? {
        val now = System.currentTimeMillis()
        byReleaseId[releaseId]?.takeIf { now - it.fetchedAtMs < TTL_MS }?.let { return it.release }

        val dto = api.getRelease(releaseId)
        val release = dto?.takeIf { it.mal?.id != null }?.let { AniLibriaMapper.toRelease(it.mal?.id ?: 0, it) }
        byReleaseId[releaseId] = Cached(release, now)
        return release
    }

    private suspend fun findReleaseId(malId: Int, titles: List<String>): Int? {
        for (title in titles.map(String::trim).filter(String::isNotEmpty).distinct().take(MAX_QUERIES)) {
            val match = api.search(title).firstOrNull {
                it.mal?.id == malId && !it.is_blocked_by_geo && !it.is_blocked_by_copyrights
            }
            if (match != null) return match.id
        }
        return null
    }

    private companion object {
        const val TTL_MS = 5 * 60 * 1000L
        const val MAX_QUERIES = 3
    }
}
