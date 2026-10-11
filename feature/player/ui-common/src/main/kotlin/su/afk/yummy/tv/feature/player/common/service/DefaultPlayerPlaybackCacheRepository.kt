package su.afk.yummy.tv.feature.player.common.service

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.ContentMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.domain.player.model.PlayerCachePurgeResult
import su.afk.yummy.tv.domain.player.model.PlayerCachePurgedEntry
import su.afk.yummy.tv.domain.player.repository.PlayerPlaybackCacheRepository
import javax.inject.Inject

/** Очищает кэш онлайн-воспроизведения ([PlayerStreamingCacheProvider]) целиком. */
@OptIn(UnstableApi::class)
class DefaultPlayerPlaybackCacheRepository @Inject constructor(
    private val cacheProvider: PlayerStreamingCacheProvider,
) : PlayerPlaybackCacheRepository {

    override suspend fun purge(): PlayerCachePurgeResult = withContext(Dispatchers.IO) {
        val cache = cacheProvider.cache
        val spaceBefore = cache.cacheSpace
        val keys = cache.keys.toList()
        val entries = keys.mapNotNull { key -> runCatching { cache.describe(key) }.getOrNull() }
        keys.forEach { key -> runCatching { cache.removeResource(key) } }
        PlayerCachePurgeResult(
            entries = keys.size,
            bytes = (spaceBefore - cache.cacheSpace).coerceAtLeast(0L),
            largest = entries.sortedByDescending { it.cachedBytes }.take(MAX_REPORTED_ENTRIES),
        )
    }

    private fun Cache.describe(key: String): PlayerCachePurgedEntry? {
        val spans = getCachedSpans(key).toList()
        if (spans.isEmpty()) return null
        return PlayerCachePurgedEntry(
            key = key.withoutQuery(),
            cachedBytes = spans.sumOf { it.length },
            contentLength = ContentMetadata.getContentLength(getContentMetadata(key)),
            spans = spans.size,
            fromPosition = spans.minOf { it.position },
            toPosition = spans.maxOf { it.position + it.length },
        )
    }

    private companion object {
        const val MAX_REPORTED_ENTRIES = 10
    }
}

/**
 * Срезает query-часть ключа: у не-okcdn источников ключ — полный URI с подписью или токеном, а лог
 * пользователь отправляет разработчику. Стабильный okcdn-ключ (`okcdn|id|type|path`) `?` не содержит.
 */
private fun String.withoutQuery(): String = substringBefore('?')
