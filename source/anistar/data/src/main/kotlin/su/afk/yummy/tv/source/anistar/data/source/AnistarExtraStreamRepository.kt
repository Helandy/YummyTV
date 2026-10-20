package su.afk.yummy.tv.source.anistar.data.source

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.anistar.data.utils.isAnistarSourceUrl
import su.afk.yummy.tv.source.anistar.data.utils.parseAnistarSourceUrl
import su.afk.yummy.tv.source.anistar.domain.model.AnistarStreams
import su.afk.yummy.tv.source.anistar.domain.model.AnistarStreamsResult
import su.afk.yummy.tv.source.anistar.domain.usecase.ResolveAnistarStreamsUseCase
import javax.inject.Inject

/** Превращает `source://anistar/…` в HLS-поток с выбором качества; CDN отдаёт плейлист только с `Referer` зеркала сайта. */
internal class AnistarExtraStreamRepository @Inject constructor(
    private val resolveStreams: ResolveAnistarStreamsUseCase,
    private val messages: AnistarMessages,
) : ExtraStreamRepository {

    override fun supports(url: String): Boolean = isAnistarSourceUrl(url)

    override suspend fun resolve(url: String): PlayerStreamResolveResult {
        val target = parseAnistarSourceUrl(url) ?: return PlayerStreamResolveResult.Failed("bad source url")
        return when (val result = resolveStreams(target)) {
            is AnistarStreamsResult.Success -> result.streams.toStream()
            AnistarStreamsResult.NotFound -> PlayerStreamResolveResult.Unavailable(messages.notFound)
            is AnistarStreamsResult.Failed -> PlayerStreamResolveResult.Failed(result.reason)
        }
    }

    private fun AnistarStreams.toStream(): PlayerStreamResolveResult.Stream {
        // Конвенция плеера: карта идёт по возрастанию качества, последнее считается лучшим.
        val ascending = hlsByQuality.entries.sortedBy { it.key }
        val qualities = LinkedHashMap<String, String>()
        ascending.forEach { qualities["${it.key}p"] = it.value }
        return PlayerStreamResolveResult.Stream(
            url = ascending.last().value,
            headers = mapOf("Referer" to referer),
            qualities = qualities,
        )
    }
}
