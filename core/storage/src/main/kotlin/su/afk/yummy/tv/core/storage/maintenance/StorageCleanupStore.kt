package su.afk.yummy.tv.core.storage.maintenance

class StorageCleanupStore(private val dao: StorageCleanupDao) {

    suspend fun purgeStaleCaches(now: Long = System.currentTimeMillis()) {
        dao.purgeCachesOlderThan(now - CACHE_RETENTION_MS)
    }

    private companion object {
        const val CACHE_RETENTION_MS = 7 * 24 * 60 * 60 * 1000L
    }
}
