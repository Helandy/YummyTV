package su.afk.yummy.tv.source.anilibria.data.source

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.anilibria.data.utils.isAniLibriaSourceUrl
import su.afk.yummy.tv.source.anilibria.data.utils.parseAniLibriaSourceUrl
import su.afk.yummy.tv.source.anilibria.data.utils.preferRuCdnHost
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaStreams
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaStreamsResult
import su.afk.yummy.tv.source.anilibria.domain.usecase.ResolveAniLibriaStreamsUseCase
import javax.inject.Inject

/** Превращает `source://anilibria/…` в HLS-поток с выбором качества. */
internal class AniLibriaExtraStreamRepository @Inject constructor(
    private val resolveStreams: ResolveAniLibriaStreamsUseCase,
    private val messages: AniLibriaMessages,
) : ExtraStreamRepository {

    override fun supports(url: String): Boolean = isAniLibriaSourceUrl(url)

    override suspend fun resolve(url: String): PlayerStreamResolveResult {
        val target = parseAniLibriaSourceUrl(url) ?: return PlayerStreamResolveResult.Failed("bad source url")
        return when (val result = resolveStreams(target)) {
            is AniLibriaStreamsResult.Success -> result.streams.toStream()
            AniLibriaStreamsResult.NotFound -> PlayerStreamResolveResult.Unavailable(messages.notFound)
            is AniLibriaStreamsResult.Failed -> PlayerStreamResolveResult.Failed(result.reason)
        }
    }

    private fun AniLibriaStreams.toStream(): PlayerStreamResolveResult.Stream {
        // Конвенция плеера: карта идёт по возрастанию качества, последнее считается лучшим.
        val ascending = files.entries.sortedBy { it.key }
        val qualities = LinkedHashMap<String, String>()
        ascending.forEach { qualities["${it.key}p"] = it.value.preferRuCdnHost() }
        return PlayerStreamResolveResult.Stream(url = ascending.last().value.preferRuCdnHost(), qualities = qualities)
    }
}
