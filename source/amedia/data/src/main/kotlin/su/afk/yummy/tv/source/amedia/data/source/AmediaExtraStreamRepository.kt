package su.afk.yummy.tv.source.amedia.data.source

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.amedia.data.utils.isAmediaSourceUrl
import su.afk.yummy.tv.source.amedia.data.utils.parseAmediaSourceUrl
import su.afk.yummy.tv.source.amedia.domain.model.AmediaStreams
import su.afk.yummy.tv.source.amedia.domain.model.AmediaStreamsResult
import su.afk.yummy.tv.source.amedia.domain.usecase.ResolveAmediaStreamsUseCase
import javax.inject.Inject

/** Превращает `source://amedia/…` в HLS-поток; заголовки не нужны: плейлисты и сегменты отдаются без Referer и подписи. */
internal class AmediaExtraStreamRepository @Inject constructor(
    private val resolveStreams: ResolveAmediaStreamsUseCase,
    private val messages: AmediaMessages,
) : ExtraStreamRepository {

    override fun supports(url: String): Boolean = isAmediaSourceUrl(url)

    override suspend fun resolve(url: String): PlayerStreamResolveResult {
        val target = parseAmediaSourceUrl(url) ?: return PlayerStreamResolveResult.Failed("bad source url")
        return when (val result = resolveStreams(target)) {
            is AmediaStreamsResult.Success -> result.streams.toStream()
            AmediaStreamsResult.NotFound -> PlayerStreamResolveResult.Unavailable(messages.notFound)
            is AmediaStreamsResult.Failed -> PlayerStreamResolveResult.Failed(result.reason)
        }
    }

    private fun AmediaStreams.toStream(): PlayerStreamResolveResult.Stream {
        if (hlsByQuality.isEmpty()) return PlayerStreamResolveResult.Stream(url = masterUrl)
        // Конвенция плеера: карта идёт по возрастанию качества, последнее считается лучшим.
        val ascending = hlsByQuality.entries.sortedBy { it.key }
        val qualities = LinkedHashMap<String, String>()
        ascending.forEach { qualities["${it.key}p"] = it.value }
        return PlayerStreamResolveResult.Stream(url = ascending.last().value, qualities = qualities)
    }
}
