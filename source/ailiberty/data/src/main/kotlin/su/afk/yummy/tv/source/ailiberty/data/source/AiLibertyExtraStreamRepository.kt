package su.afk.yummy.tv.source.ailiberty.data.source

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.ailiberty.data.utils.isAiLibertySourceUrl
import su.afk.yummy.tv.source.ailiberty.data.utils.parseAiLibertySourceUrl
import su.afk.yummy.tv.source.ailiberty.domain.model.AiLibertyStreamsResult
import su.afk.yummy.tv.source.ailiberty.domain.usecase.ResolveAiLibertyStreamsUseCase
import javax.inject.Inject

/**
 * Превращает `source://ailiberty/…` в HLS-поток. Каждое качество — отдельный плейлист, мастера нет, поэтому плееру отдаётся
 * карта качеств по возрастанию (лучшее по умолчанию — последнее). Заголовков не нужно.
 */
internal class AiLibertyExtraStreamRepository @Inject constructor(
    private val resolveStreams: ResolveAiLibertyStreamsUseCase,
    private val messages: AiLibertyMessages,
) : ExtraStreamRepository {

    override fun supports(url: String): Boolean = isAiLibertySourceUrl(url)

    override suspend fun resolve(url: String): PlayerStreamResolveResult {
        val target = parseAiLibertySourceUrl(url) ?: return PlayerStreamResolveResult.Failed("bad source url")
        return when (val result = resolveStreams(target)) {
            is AiLibertyStreamsResult.Success -> PlayerStreamResolveResult.Stream(
                url = result.streams.qualities.values.last(),
                qualities = LinkedHashMap(result.streams.qualities),
            )
            AiLibertyStreamsResult.NotFound -> PlayerStreamResolveResult.Unavailable(messages.notFound)
            is AiLibertyStreamsResult.Failed -> PlayerStreamResolveResult.Failed(result.reason)
        }
    }
}
