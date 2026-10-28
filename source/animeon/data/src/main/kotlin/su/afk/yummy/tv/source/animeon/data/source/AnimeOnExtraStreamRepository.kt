package su.afk.yummy.tv.source.animeon.data.source

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.animeon.data.utils.isAnimeOnSourceUrl
import su.afk.yummy.tv.source.animeon.data.utils.parseAnimeOnSourceUrl
import su.afk.yummy.tv.source.animeon.domain.usecase.ResolveAnimeOnStreamsUseCase
import su.afk.yummy.tv.source.ashdi.domain.model.AshdiStreams
import su.afk.yummy.tv.source.ashdi.domain.model.AshdiStreamsResult
import javax.inject.Inject

/**
 * Превращает `source://animeon/…` в HLS-поток Ashdi. Заголовков плееру не нужно. Мастер разобран на варианты: отдаётся карта
 * качеств по возрастанию (лучшее по умолчанию — последнее), иначе мастер играет адаптивно («Авто»).
 */
internal class AnimeOnExtraStreamRepository @Inject constructor(
    private val resolveStreams: ResolveAnimeOnStreamsUseCase,
    private val messages: AnimeOnMessages,
) : ExtraStreamRepository {

    override fun supports(url: String): Boolean = isAnimeOnSourceUrl(url)

    override suspend fun resolve(url: String): PlayerStreamResolveResult {
        val target = parseAnimeOnSourceUrl(url) ?: return PlayerStreamResolveResult.Failed("bad source url")
        return when (val result = resolveStreams(target)) {
            is AshdiStreamsResult.Success -> result.streams.toStream()
            AshdiStreamsResult.RegionBlocked -> PlayerStreamResolveResult.Unavailable(messages.regionBlocked)
            AshdiStreamsResult.NotFound -> PlayerStreamResolveResult.Unavailable(messages.notFound)
            is AshdiStreamsResult.Failed -> PlayerStreamResolveResult.Failed(result.reason)
        }
    }

    private fun AshdiStreams.toStream(): PlayerStreamResolveResult.Stream {
        if (qualities.isEmpty()) return PlayerStreamResolveResult.Stream(url = masterUrl)
        // Конвенция плеера: карта идёт по возрастанию качества, последнее считается лучшим.
        return PlayerStreamResolveResult.Stream(url = qualities.values.last(), qualities = LinkedHashMap(qualities))
    }
}
