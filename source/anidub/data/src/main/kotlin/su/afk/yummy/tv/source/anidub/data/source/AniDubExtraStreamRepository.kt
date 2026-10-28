package su.afk.yummy.tv.source.anidub.data.source

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.anidub.data.utils.isAniDubSourceUrl
import su.afk.yummy.tv.source.anidub.data.utils.parseAniDubSourceUrl
import su.afk.yummy.tv.source.anidub.domain.model.AniDubStreams
import su.afk.yummy.tv.source.anidub.domain.model.AniDubStreamsResult
import su.afk.yummy.tv.source.anidub.domain.usecase.ResolveAniDubStreamsUseCase
import javax.inject.Inject

/**
 * Превращает `source://anidub/…` в HLS-поток. Мастер серии разобран на варианты: плееру отдаётся карта качеств по возрастанию
 * (лучшее по умолчанию — последнее). Если варианты не разобрались, играет сам мастер адаптивно («Авто»). Заголовки обязательны:
 * ссылки CDN подписаны User-Agent, с которым запрошен мастер, и с другим UA отвечают 403.
 */
internal class AniDubExtraStreamRepository @Inject constructor(
    private val resolveStreams: ResolveAniDubStreamsUseCase,
    private val messages: AniDubMessages,
) : ExtraStreamRepository {

    override fun supports(url: String): Boolean = isAniDubSourceUrl(url)

    override suspend fun resolve(url: String): PlayerStreamResolveResult {
        val target = parseAniDubSourceUrl(url) ?: return PlayerStreamResolveResult.Failed("bad source url")
        return when (val result = resolveStreams(target)) {
            is AniDubStreamsResult.Success -> result.streams.toStream()
            AniDubStreamsResult.NotFound -> PlayerStreamResolveResult.Unavailable(messages.notFound)
            is AniDubStreamsResult.Failed -> PlayerStreamResolveResult.Failed(result.reason)
        }
    }

    private fun AniDubStreams.toStream(): PlayerStreamResolveResult.Stream {
        if (qualities.isEmpty()) return PlayerStreamResolveResult.Stream(url = masterUrl, headers = headers)
        // Конвенция плеера: карта идёт по возрастанию качества, последнее считается лучшим.
        return PlayerStreamResolveResult.Stream(url = qualities.values.last(), headers = headers, qualities = LinkedHashMap(qualities))
    }
}
