package su.afk.yummy.tv.data.player.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.data.player.extractor.AllohaExtractor
import su.afk.yummy.tv.data.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.PlayerStreamRepository
import su.afk.yummy.tv.domain.player.session.AllohaPlaybackSessionManager
import javax.inject.Inject

internal class DefaultPlayerStreamRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val extractors: Set<@JvmSuppressWildcards PlayerStreamExtractor>,
    private val allohaExtractor: AllohaExtractor,
    private val allohaSessionManager: AllohaPlaybackSessionManager,
) : PlayerStreamRepository {

    override suspend fun resolve(request: PlayerStreamRequest): PlayerStreamResolveResult {
        val url = request.iframeUrl
        return extractors.firstOrNull { it.supports(url) }
            ?.extract(request, context)
            ?: PlayerStreamResolveResult.Unsupported
    }

    override suspend fun openAllohaSession(request: PlayerStreamRequest): AllohaStreamSession? =
        if (request.reusePlaybackSession) {
            allohaSessionManager.find(request.iframeUrl)
                ?: allohaExtractor.openSession(request, context)
                    ?.let(allohaSessionManager::activate)
        } else {
            allohaExtractor.openSession(request, context)
        }
}
