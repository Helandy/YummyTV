package su.afk.yummy.tv.source.studiomir.data.source

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.studiomir.data.utils.isStudioMirSourceUrl
import su.afk.yummy.tv.source.studiomir.data.utils.parseStudioMirSourceUrl
import su.afk.yummy.tv.source.studiomir.domain.model.StudioMirStreams
import su.afk.yummy.tv.source.studiomir.domain.model.StudioMirStreamsResult
import su.afk.yummy.tv.source.studiomir.domain.usecase.ResolveStudioMirStreamsUseCase
import javax.inject.Inject

/**
 * Превращает `source://studiomir/…` в HLS-поток. Плееру отдаётся карта качеств по возрастанию (лучшее по умолчанию — последнее).
 * Ссылки Studio MIR не подписаны и заголовков не требуют.
 */
internal class StudioMirExtraStreamRepository @Inject constructor(
    private val resolveStreams: ResolveStudioMirStreamsUseCase,
    private val messages: StudioMirMessages,
) : ExtraStreamRepository {

    override fun supports(url: String): Boolean = isStudioMirSourceUrl(url)

    override suspend fun resolve(url: String): PlayerStreamResolveResult {
        val target = parseStudioMirSourceUrl(url) ?: return PlayerStreamResolveResult.Failed("bad source url")
        return when (val result = resolveStreams(target)) {
            is StudioMirStreamsResult.Success -> result.streams.toStream()
            StudioMirStreamsResult.NotFound -> PlayerStreamResolveResult.Unavailable(messages.notFound)
            is StudioMirStreamsResult.Failed -> PlayerStreamResolveResult.Failed(result.reason)
        }
    }

    private fun StudioMirStreams.toStream(): PlayerStreamResolveResult.Stream {
        if (qualities.isEmpty()) return PlayerStreamResolveResult.Stream(url = masterUrl, headers = headers)
        // Конвенция плеера: карта идёт по возрастанию качества, последнее считается лучшим.
        return PlayerStreamResolveResult.Stream(url = qualities.values.last(), headers = headers, qualities = LinkedHashMap(qualities))
    }
}
