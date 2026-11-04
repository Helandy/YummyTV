package su.afk.yummy.tv.source.sameband.data.source

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.sameband.data.utils.isSamebandSourceUrl
import su.afk.yummy.tv.source.sameband.data.utils.parseSamebandSourceUrl
import su.afk.yummy.tv.source.sameband.domain.model.SamebandStreams
import su.afk.yummy.tv.source.sameband.domain.model.SamebandStreamsResult
import su.afk.yummy.tv.source.sameband.domain.usecase.ResolveSamebandStreamsUseCase
import javax.inject.Inject

/** Превращает `source://sameband/…` в HLS-поток с картой качеств по возрастанию (лучшее по умолчанию — последнее). */
internal class SamebandExtraStreamRepository @Inject constructor(
    private val resolveStreams: ResolveSamebandStreamsUseCase,
    private val messages: SamebandMessages,
) : ExtraStreamRepository {

    override fun supports(url: String): Boolean = isSamebandSourceUrl(url)

    override suspend fun resolve(url: String): PlayerStreamResolveResult {
        val target = parseSamebandSourceUrl(url) ?: return PlayerStreamResolveResult.Failed("bad source url")
        return when (val result = resolveStreams(target)) {
            is SamebandStreamsResult.Success -> result.streams.toStream()
            SamebandStreamsResult.NotFound -> PlayerStreamResolveResult.Unavailable(messages.notFound)
            is SamebandStreamsResult.Failed -> PlayerStreamResolveResult.Failed(result.reason)
        }
    }

    private fun SamebandStreams.toStream(): PlayerStreamResolveResult.Stream =
        PlayerStreamResolveResult.Stream(url = qualities.values.last(), headers = headers, qualities = LinkedHashMap(qualities))
}
