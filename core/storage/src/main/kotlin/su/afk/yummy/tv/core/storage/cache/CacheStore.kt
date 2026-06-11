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
        forceRefresh: Boolean = false,
    ): T {
        if (!forceRefresh) {
            val cached = readCached(key, deserialize, isValid)
            if (cached?.isFresh(ttlMs) == true) return cached.value
        }

        val lock = locks.getOrPut(key) { Mutex() }
        return lock.withLock {
            val cached = readCached(key, deserialize, isValid)
            if (!forceRefresh) {
                if (cached?.isFresh(ttlMs) == true) return@withLock cached.value
            }

            runCatching { fetch() }.fold(
                onSuccess = { fresh ->
                    if (isValid(fresh)) {
                        put(key, serialize, fresh)
                    }
                    fresh
                },
                onFailure = { error ->
                    cached?.let { return@withLock it.value }
                    throw error
                },
            )
        }
    }

    suspend fun <T> put(
        key: String,
        serialize: (T) -> String,
        value: T,
    ) {
        dao.put(
            CacheEntry(
                key = key,
                json = serialize(value),
                cachedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun invalidate(key: String) {
        dao.delete(key)
    }

    suspend fun invalidatePrefix(prefix: String) {
        dao.deleteByPrefix(prefix)
    }

    private data class CachedValue<T>(
        val value: T,
        val cachedAt: Long,
    ) {
        fun isFresh(ttlMs: Long): Boolean =
            System.currentTimeMillis() - cachedAt < ttlMs
    }

    private suspend fun <T> readCached(
        key: String,
        deserialize: (String) -> T,
        isValid: (T) -> Boolean,
    ): CachedValue<T>? {
        val entry = dao.get(key) ?: return null
        val cached = runCatching { deserialize(entry.json) }.getOrElse {
            dao.delete(key)
            return null
        }
        if (!isValid(cached)) {
            dao.delete(key)
            return null
        }
        return CachedValue(value = cached, cachedAt = entry.cachedAt)
    }

    suspend fun pruneOlderThan(cutoffMs: Long) {
        dao.deleteOlderThan(cutoffMs)
    }
}
