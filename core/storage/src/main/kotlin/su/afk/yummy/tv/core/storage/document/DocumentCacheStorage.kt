package su.afk.yummy.tv.core.storage.document

/** Абстракция над generic-кэшем документов/HTML-страниц — позволяет подменять реализацию в тестах. */
interface DocumentCacheStorage {

    suspend fun <T> getOrFetch(
        cacheKey: String,
        ttlMs: Long,
        forceRefresh: Boolean = false,
        decode: (String) -> T,
        encode: (T) -> String,
        fetch: suspend () -> T,
    ): T

    suspend fun read(cacheKey: String): String?

    suspend fun delete(cacheKey: String)

    suspend fun deleteByPrefix(prefix: String)

    suspend fun deleteUserNamespace(namespace: String)
}
