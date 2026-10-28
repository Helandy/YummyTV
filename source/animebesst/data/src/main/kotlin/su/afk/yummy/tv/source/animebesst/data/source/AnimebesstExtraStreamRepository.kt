package su.afk.yummy.tv.source.animebesst.data.source

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.animebesst.data.utils.isAnimebesstSourceUrl
import su.afk.yummy.tv.source.animebesst.data.utils.parseAnimebesstSourceUrl
import su.afk.yummy.tv.source.animebesst.domain.model.AnimebesstStreams
import su.afk.yummy.tv.source.animebesst.domain.model.AnimebesstStreamsResult
import su.afk.yummy.tv.source.animebesst.domain.usecase.ResolveAnimebesstStreamsUseCase
import javax.inject.Inject

/**
 * Превращает `source://animebesst/…` в HLS-поток. Заголовков плееру не нужно. Мастер разобран на варианты: отдаётся карта качеств
 * по возрастанию (лучшее по умолчанию — последнее), иначе мастер играет адаптивно («Авто»).
 */
internal class AnimebesstExtraStreamRepository @Inject constructor(
    private val resolveStreams: ResolveAnimebesstStreamsUseCase,
    private val messages: AnimebesstMessages,
) : ExtraStreamRepository {

    override fun supports(url: String): Boolean = isAnimebesstSourceUrl(url)

    override suspend fun resolve(url: String): PlayerStreamResolveResult {
        val target = parseAnimebesstSourceUrl(url) ?: return PlayerStreamResolveResult.Failed("bad source url")
        return when (val result = resolveStreams(target)) {
            is AnimebesstStreamsResult.Success -> result.streams.toStream()
            AnimebesstStreamsResult.NotFound -> PlayerStreamResolveResult.Unavailable(messages.notFound)
            is AnimebesstStreamsResult.Failed -> PlayerStreamResolveResult.Failed(result.reason)
        }
    }

    private fun AnimebesstStreams.toStream(): PlayerStreamResolveResult.Stream {
        if (qualities.isEmpty()) return PlayerStreamResolveResult.Stream(url = masterUrl)
        // Конвенция плеера: карта идёт по возрастанию качества, последнее считается лучшим.
        return PlayerStreamResolveResult.Stream(url = qualities.values.last(), qualities = LinkedHashMap(qualities))
    }
}
