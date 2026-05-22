package su.afk.yummy.tv.core.storage.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class CacheStore(private val dao: CacheDao) {

    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun <T> getOrFetch(
        key: String,
        ttlMs: Long,
        serialize: (T) -> String,
        deserialize: (String) -> T,
        fetch: suspend () -> T,
        isValid: (T) -> Boolean = { true },
    ): T {
        readValidCached(key, ttlMs, deserialize, isValid)?.let { return it }

        val lock = locks.getOrPut(key) { Mutex() }
        return lock.withLock {
            readValidCached(key, ttlMs, deserialize, isValid)?.let { return@withLock it }

            val fresh = fetch()
            if (isValid(fresh)) {
                dao.put(CacheEntry(key = key, json = serialize(fresh), cachedAt = System.currentTimeMillis()))
            }
            fresh
        }
    }

    private suspend fun <T> readValidCached(
        key: String,
        ttlMs: Long,
        deserialize: (String) -> T,
        isValid: (T) -> Boolean,
    ): T? {
        val entry = dao.get(key) ?: return null
        if (System.currentTimeMillis() - entry.cachedAt >= ttlMs) return null

        val cached = runCatching { deserialize(entry.json) }.getOrElse {
            dao.delete(key)
            return null
        }
        if (!isValid(cached)) {
            dao.delete(key)
            return null
        }
        return cached
    }
}
