package su.afk.yummy.tv.core.storage.document

import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import javax.inject.Inject

/**
 * User- и language-скоупный кэш поверх [DocumentCacheStorage].
 *
 * Строит ключ вида `user:$userId:$namespace:$language:$key` (гость — `user:0:…`) и
 * (де)сериализует значения через [DocumentCacheJson]. Инкапсулирует boilerplate, ранее
 * продублированный в репозиториях reviews/posts/bloggers.
 */
class UserScopedCache @Inject constructor(
    @PublishedApi internal val cache: DocumentCacheStorage,
    @PublishedApi internal val settings: YaniAccountSettingsStore,
) {

    /** Префикс `user:$userId:$namespace:$language:` для текущего пользователя и языка. */
    @PublishedApi
    internal suspend fun prefix(namespace: String): String {
        val userId = settings.yaniUserId.first().coerceAtLeast(0)
        val language = settings.yaniContentLanguage.first().apiCode
        return "user:$userId:$namespace:$language:"
    }

    /** Возвращает закэшированное значение или загружает через [fetch] и кэширует под [key]. */
    suspend inline fun <reified T> cached(
        namespace: String,
        key: String,
        ttlMs: Long,
        forceRefresh: Boolean = false,
        crossinline fetch: suspend () -> T,
    ): T = cache.getOrFetchJson(
        cacheKey = prefix(namespace) + key,
        ttlMs = ttlMs,
        forceRefresh = forceRefresh,
        fetch = { fetch() },
    )

    /** Читает сохранённое значение независимо от срока его действия. */
    suspend inline fun <reified T> read(namespace: String, key: String): T? {
        val payload = cache.read(prefix(namespace) + key) ?: return null
        return runCatching { DocumentCacheJson.decodeFromString<T>(payload) }.getOrNull()
    }

    /** Удаляет один ключ в namespace текущего пользователя. */
    suspend fun delete(namespace: String, key: String) =
        cache.delete(prefix(namespace) + key)

    /** Удаляет все ключи с данным префиксом внутри namespace текущего пользователя. */
    suspend fun deleteByPrefix(namespace: String, keyPrefix: String = "") =
        cache.deleteByPrefix(prefix(namespace) + keyPrefix)

    /** Удаляет весь namespace для всех пользователей (broad invalidation). */
    suspend fun deleteUserNamespace(namespace: String) =
        cache.deleteUserNamespace(namespace)
}
