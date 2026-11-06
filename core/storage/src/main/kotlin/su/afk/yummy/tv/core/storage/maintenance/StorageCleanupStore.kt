package su.afk.yummy.tv.core.storage.maintenance

internal class StorageCleanupStore(private val dao: StorageCleanupDao) : StorageCleanup {

    override suspend fun purgeStaleCaches(now: Long) {
        dao.purgeCachesOlderThan(now - CACHE_RETENTION_MS)
    }

    override suspend fun purgeAllCaches() {
        dao.purgeCachesOlderThan(Long.MAX_VALUE)
    }

    private companion object {
        const val CACHE_RETENTION_MS = 7 * 24 * 60 * 60 * 1000L
    }
}
