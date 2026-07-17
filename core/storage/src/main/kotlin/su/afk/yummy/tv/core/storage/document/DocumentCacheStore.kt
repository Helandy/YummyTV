package su.afk.yummy.tv.core.storage.document

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class DocumentCacheStore(private val dao: DocumentCacheDao) {
    private val requestLocks = ConcurrentHashMap<String, RequestLock>()
    private val invalidationVersion = AtomicLong()

    suspend fun <T> getOrFetch(
        cacheKey: String,
        ttlMs: Long,
        forceRefresh: Boolean = false,
        decode: (String) -> T,
        encode: (T) -> String,
        fetch: suspend () -> T,
    ): T {
        val requestLock = requestLocks.compute(cacheKey) { _, current ->
            (current ?: RequestLock()).also { it.users.incrementAndGet() }
        } ?: error("Could not create cache request lock")
        return try {
            requestLock.mutex.withLock {
                val stored = dao.get(cacheKey)
                val decoded = stored?.payload?.let { payload ->
                    runCatching { decode(payload) }.getOrNull()
                }
                val ageMs = stored?.let { System.currentTimeMillis() - it.cachedAt }
                val fresh = ageMs != null && ageMs in 0..ttlMs
                if (!forceRefresh && fresh && decoded != null) return@withLock decoded

                val versionBeforeFetch = invalidationVersion.get()
                try {
                    fetch().also { value ->
                        if (invalidationVersion.get() == versionBeforeFetch) {
                            dao.save(
                                DocumentCacheEntry(
                                    cacheKey = cacheKey,
                                    payload = encode(value),
                                    cachedAt = System.currentTimeMillis(),
                                )
                            )
                        }
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    decoded ?: throw error
                }
            }
        } finally {
            if (requestLock.users.decrementAndGet() == 0) {
                requestLocks.remove(cacheKey, requestLock)
            }
        }
    }

    private class RequestLock(
        val mutex: Mutex = Mutex(),
        val users: AtomicInteger = AtomicInteger(),
    )

    suspend fun delete(cacheKey: String) {
        invalidationVersion.incrementAndGet()
        dao.delete(cacheKey)
    }

    suspend fun deleteByPrefix(prefix: String) {
        invalidationVersion.incrementAndGet()
        dao.deleteByPrefix(prefix)
    }

    suspend fun deleteUserNamespace(namespace: String) {
        invalidationVersion.incrementAndGet()
        dao.deleteUserNamespace(namespace)
    }
}
