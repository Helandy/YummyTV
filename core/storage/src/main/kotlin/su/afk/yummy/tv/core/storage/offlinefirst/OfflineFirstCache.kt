package su.afk.yummy.tv.core.storage.offlinefirst

import kotlinx.coroutines.CancellationException

/**
 * Orchestration for the project's offline-first pattern: read a TTL-cached [Cache] entity,
 * serve it if fresh, otherwise fetch from the network (persisting the result), and fall back
 * to a stale cache on network failure rather than surfacing the error when possible.
 *
 * Each domain keeps its own typed Room table/entity and its own `isFresh(ttlMs)` extension;
 * this helper only removes the copy-pasted try/catch/fallback wiring around them, so [read],
 * [fetchAndSave] and friends are expected to talk to that domain's existing Storage/Api.
 *
 * Unlike the JSON-blob cache in `DocumentCacheStore`, this helper does not deduplicate
 * concurrent calls (no per-key mutex) — the repositories it replaces never had that either,
 * so adding it here would be a behavior change beyond removing duplication.
 */
suspend fun <Cache, Domain> offlineFirstCache(
    forceRefresh: Boolean = false,
    isOnline: () -> Boolean = { true },
    read: suspend () -> Cache?,
    isFresh: (Cache) -> Boolean,
    toDomain: (Cache) -> Domain,
    fetchAndSave: suspend () -> Cache,
    transform: suspend (Domain) -> Domain = { it },
    onMissing: suspend (Throwable) -> Domain = { throw it },
): Domain {
    val stored = if (forceRefresh) null else read()
    if (stored != null && (isFresh(stored) || !isOnline())) {
        return transform(toDomain(stored))
    }

    return try {
        transform(toDomain(fetchAndSave()))
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        // Explicit null-check (not `?.let { } ?: onMissing()`): Domain can itself be a nullable
        // type (e.g. Int?), and a real cache hit whose mapped value happens to be null must not
        // be mistaken for "no cache" and routed into onMissing().
        val fallbackCache = stored ?: if (forceRefresh) read() else null
        if (fallbackCache != null) transform(toDomain(fallbackCache)) else onMissing(error)
    }
}
