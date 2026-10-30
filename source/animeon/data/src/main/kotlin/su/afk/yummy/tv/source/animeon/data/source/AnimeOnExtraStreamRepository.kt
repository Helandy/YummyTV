package su.afk.yummy.tv.source.animeon.data.source

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.animeon.data.utils.isAnimeOnSourceUrl
import su.afk.yummy.tv.source.animeon.data.utils.parseAnimeOnSourceUrl
import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnStreams
import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnStreamsResult
import su.afk.yummy.tv.source.animeon.domain.usecase.ResolveAnimeOnStreamsUseCase
import javax.inject.Inject

/**
 * Превращает `source://animeon/…` в поток плеера (Ashdi, Fenix или Moon). Заголовки нужны только Moon. Мастер разобран на варианты:
 * отдаётся карта качеств по возрастанию (лучшее по умолчанию — последнее), иначе мастер играет адаптивно («Авто»).
 */
internal class AnimeOnExtraStreamRepository @Inject constructor(
    private val resolveStreams: ResolveAnimeOnStreamsUseCase,
    private val messages: AnimeOnMessages,
) : ExtraStreamRepository {

    override fun supports(url: String): Boolean = isAnimeOnSourceUrl(url)

    override suspend fun resolve(url: String): PlayerStreamResolveResult {
        val target = parseAnimeOnSourceUrl(url) ?: return PlayerStreamResolveResult.Failed("bad source url")
        return when (val result = resolveStreams(target)) {
            is AnimeOnStreamsResult.Success -> result.streams.toStream()
            AnimeOnStreamsResult.RegionBlocked -> PlayerStreamResolveResult.Unavailable(messages.regionBlocked)
            AnimeOnStreamsResult.NotFound -> PlayerStreamResolveResult.Unavailable(messages.notFound)
            is AnimeOnStreamsResult.Failed -> PlayerStreamResolveResult.Failed(result.reason)
        }
    }

    private fun AnimeOnStreams.toStream(): PlayerStreamResolveResult.Stream {
        if (qualities.isEmpty()) return PlayerStreamResolveResult.Stream(url = masterUrl, headers = headers)
        // Конвенция плеера: карта идёт по возрастанию качества, последнее считается лучшим.
        return PlayerStreamResolveResult.Stream(url = qualities.values.last(), headers = headers, qualities = LinkedHashMap(qualities))
    }
}
