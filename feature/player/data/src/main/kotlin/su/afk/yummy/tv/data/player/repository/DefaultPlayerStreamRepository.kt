package su.afk.yummy.tv.data.player.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.data.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.data.player.extractor.SessionAwarePlayerStreamExtractor
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.AllohaPlaybackSessionRepository
import su.afk.yummy.tv.domain.player.repository.PlayerStreamRepository
import javax.inject.Inject

internal class DefaultPlayerStreamRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val extractors: Set<@JvmSuppressWildcards PlayerStreamExtractor>,
    private val allohaSessionManager: AllohaPlaybackSessionRepository,
) : PlayerStreamRepository {

    private val sessionAwareExtractors: List<SessionAwarePlayerStreamExtractor> =
        extractors.filterIsInstance<SessionAwarePlayerStreamExtractor>()

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
        // Session-aware стримы (сейчас только Alloha) живут внутри playback-сессии (прокси,
        // ротация подписанных манифестов), их кэшировать по URL нельзя — сессией управляет
        // AllohaPlaybackSessionRepository.
        if (extractor is SessionAwarePlayerStreamExtractor) return extractor.extract(
            request,
            context
        )

        val cacheKey = "$url|${request.autoQualityLabel}"
        if (!request.forceRefresh) {
            cachedStream(cacheKey)?.let { return it }
        }

        val result = extractor.extract(request, context)
        if (result is PlayerStreamResolveResult.Stream) {
            rememberStream(cacheKey, result)
        }
        return result
    }

    override suspend fun openAllohaSession(request: PlayerStreamRequest): AllohaStreamSession? {
        val extractor = sessionAwareExtractors.firstOrNull { it.supports(request.iframeUrl) }
            ?: return null
        return if (request.reusePlaybackSession) {
            allohaSessionManager.find(request.iframeUrl)
                ?: extractor.openSession(request, context)
                    ?.let(allohaSessionManager::activate)
        } else {
            extractor.openSession(request, context)
        }
    }

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
