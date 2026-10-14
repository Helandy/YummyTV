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
        crossinline fetch: suspend () -> T,
    ): T = cache.getOrFetchJson(
        cacheKey = prefix(namespace) + key,
        ttlMs = ttlMs,
        fetch = { fetch() },
    )

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
