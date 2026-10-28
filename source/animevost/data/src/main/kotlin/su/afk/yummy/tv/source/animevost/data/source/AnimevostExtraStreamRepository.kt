package su.afk.yummy.tv.source.animevost.data.source

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.animevost.data.utils.isAnimevostSourceUrl
import su.afk.yummy.tv.source.animevost.data.utils.parseAnimevostSourceUrl
import su.afk.yummy.tv.source.animevost.domain.model.AnimevostStreamsResult
import su.afk.yummy.tv.source.animevost.domain.usecase.ResolveAnimevostStreamsUseCase
import javax.inject.Inject

/**
 * Превращает `source://animevost/…` в прямой mp4. Заголовков плееру не нужно. Плееру отдаётся карта качеств по возрастанию
 * (лучшее по умолчанию — последнее).
 */
internal class AnimevostExtraStreamRepository @Inject constructor(
    private val resolveStreams: ResolveAnimevostStreamsUseCase,
    private val messages: AnimevostMessages,
) : ExtraStreamRepository {

    override fun supports(url: String): Boolean = isAnimevostSourceUrl(url)

    override suspend fun resolve(url: String): PlayerStreamResolveResult {
        val target = parseAnimevostSourceUrl(url) ?: return PlayerStreamResolveResult.Failed("bad source url")
        return when (val result = resolveStreams(target)) {
            is AnimevostStreamsResult.Success ->
                // Конвенция плеера: карта идёт по возрастанию качества, последнее считается лучшим.
                PlayerStreamResolveResult.Stream(url = result.streams.qualities.values.last(), qualities = LinkedHashMap(result.streams.qualities))
            AnimevostStreamsResult.NotFound -> PlayerStreamResolveResult.Unavailable(messages.notFound)
            is AnimevostStreamsResult.Failed -> PlayerStreamResolveResult.Failed(result.reason)
        }
    }
}
