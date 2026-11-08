// Part of YummyTV — https://github.com/Helandy/yummy-tv (CC BY-NC 4.0, see NOTICE)
package su.afk.yummy.tv.data.player.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.core.common.player.isExtraSourceUrl
import su.afk.yummy.tv.data.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.core.common.player.ResolveProgress
import su.afk.yummy.tv.domain.player.repository.PlayerStreamRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultPlayerStreamRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val extractors: Set<@JvmSuppressWildcards PlayerStreamExtractor>,
) : PlayerStreamRepository {

    private val resolveCache = object : LinkedHashMap<String, CachedStream>(
        RESOLVE_CACHE_MAX_ENTRIES,
        0.75f,
        true,
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, CachedStream>,
        ): Boolean = size > RESOLVE_CACHE_MAX_ENTRIES
    }

    override suspend fun resolve(request: PlayerStreamRequest): PlayerStreamResolveResult {
        val url = request.iframeUrl
        val extractor = extractors.firstOrNull { it.supports(url) }
            ?: return PlayerStreamResolveResult.Unsupported
        // Источники модулей (`source://`) сами решают, когда нужен новый доступ: ссылку в общий кэш не кладём.
        if (url.isExtraSourceUrl()) return extractWithProgress(extractor, request)

        val cacheKey = "$url|${request.autoQualityLabel}"
        if (!request.forceRefresh) {
            cachedStream(cacheKey)?.let { return it }
        }

        val result = extractWithProgress(extractor, request)
        if (result is PlayerStreamResolveResult.Stream) {
            rememberStream(cacheKey, result)
        }
        return result
    }

    /**
     * Экстракторы без собственных шагов (Rutube, Sibnet, VK, источники `source://`…) получают шаги от
     * сетевого плагина: он читает [ResolveProgress] из контекста корутины.
     */
    private suspend fun extractWithProgress(
        extractor: PlayerStreamExtractor,
        request: PlayerStreamRequest,
    ): PlayerStreamResolveResult =
        withContext(
            ResolveProgress(
                onStatus = request.onStatus,
                onDetail = request.onDetail,
                emitsSteps = !extractor.reportsOwnProgress,
            ),
        ) { extractor.extract(request, context) }

    // Ключ включает autoQualityLabel, поэтому удаляется весь набор записей источника.
    override fun invalidateResolveCache(iframeUrl: String) {
        val keyPrefix = "$iframeUrl|"
        synchronized(resolveCache) {
            resolveCache.keys.removeAll { it.startsWith(keyPrefix) }
        }
    }

    private fun cachedStream(key: String): PlayerStreamResolveResult.Stream? =
        synchronized(resolveCache) {
            val cached = resolveCache[key] ?: return null
            if (System.currentTimeMillis() - cached.resolvedAt > RESOLVE_CACHE_TTL_MS) {
                resolveCache.remove(key)
                null
            } else {
                cached.stream
            }
        }

    private fun rememberStream(key: String, stream: PlayerStreamResolveResult.Stream) {
        synchronized(resolveCache) {
            resolveCache[key] = CachedStream(stream, System.currentTimeMillis())
        }
    }

    private data class CachedStream(
        val stream: PlayerStreamResolveResult.Stream,
        val resolvedAt: Long,
    )

    private companion object {
        // Ссылки на стримы подписанные и протухают, поэтому TTL короткий: достаточно,
        // чтобы не репарсить iframe при смене качества или повторном входе в плеер.
        const val RESOLVE_CACHE_TTL_MS = 3 * 60 * 1000L
        const val RESOLVE_CACHE_MAX_ENTRIES = 8
    }
}
