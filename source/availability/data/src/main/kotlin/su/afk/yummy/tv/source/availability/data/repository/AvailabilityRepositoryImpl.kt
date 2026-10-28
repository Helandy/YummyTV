package su.afk.yummy.tv.source.availability.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import su.afk.yummy.tv.source.availability.data.mapper.AvailabilityMapper
import su.afk.yummy.tv.source.availability.data.network.AvailabilityApi
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityAnswer
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.repository.AvailabilityRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Каждый балансер спрашивает наличие отдельно, а сервер отвечает за все сразу, поэтому запросы по одному тайтлу сливаются:
 * первый идёт на сервер, остальные ждут его под замком и берут готовый ответ. Ответ, в котором часть источников не ответила,
 * не кэшируется, чтобы следующий запрос их переспросил; сетевой сбой не кэшируется вовсе.
 */
@Singleton
internal class AvailabilityRepositoryImpl @Inject constructor(
    private val api: AvailabilityApi,
) : AvailabilityRepository {

    private data class Cached(val answer: AvailabilityAnswer, val fetchedAtMs: Long)

    private val cache = ConcurrentHashMap<String, Cached>()
    private val locks = ConcurrentHashMap<String, Mutex>()

    override suspend fun find(lookup: AvailabilityLookup): AvailabilityAnswer {
        val key = cacheKey(lookup)
        fresh(key)?.let { return it }
        return locks.getOrPut(key) { Mutex() }.withLock {
            fresh(key)?.let { return@withLock it }
            val answer = AvailabilityMapper.toAnswer(api.find(AvailabilityMapper.toRequest(lookup)))
            if (answer.failedSourceIds.isEmpty()) cache[key] = Cached(answer, System.currentTimeMillis())
            answer
        }
    }

    private fun fresh(key: String): AvailabilityAnswer? =
        cache[key]?.takeIf { System.currentTimeMillis() - it.fetchedAtMs < TTL_MS }?.answer

    private fun cacheKey(lookup: AvailabilityLookup): String = lookup.malId?.let { "mal:$it" } ?: "title:${lookup.title}|${lookup.year}"

    private companion object {
        const val TTL_MS = 5 * 60 * 1000L
    }
}
