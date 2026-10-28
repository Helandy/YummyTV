package su.afk.yummy.tv.source.mikai.data.source

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.ashdi.domain.model.AshdiStreams
import su.afk.yummy.tv.source.ashdi.domain.model.AshdiStreamsResult
import su.afk.yummy.tv.source.ashdi.domain.usecase.ResolveAshdiStreamsUseCase
import su.afk.yummy.tv.source.fenix.domain.model.FenixStreamsResult
import su.afk.yummy.tv.source.fenix.domain.usecase.ResolveFenixStreamsUseCase
import su.afk.yummy.tv.source.mikai.data.utils.isMikaiSourceUrl
import su.afk.yummy.tv.source.mikai.data.utils.mikaiAshdiPageUrl
import su.afk.yummy.tv.source.mikai.data.utils.mikaiFenixPageUrl
import su.afk.yummy.tv.source.mikai.data.utils.parseMikaiFenixSourceUrl
import su.afk.yummy.tv.source.mikai.data.utils.parseMikaiSourceUrl
import javax.inject.Inject

/**
 * Превращает `source://mikai/ashdi/{id}` в HLS-поток Ashdi, а `source://mikai/fenix/{токен}` — в HLS-мастер Fenix с токеном
 * доступа. Заголовков плееру не нужно. Мастер Ashdi разобран на варианты: отдаётся карта качеств по возрастанию (лучшее по
 * умолчанию — последнее), иначе мастер играет адаптивно («Авто»). У Fenix звук отдельной дорожкой мастера, он всегда адаптивный.
 */
internal class MikaiExtraStreamRepository @Inject constructor(
    private val resolveStreams: ResolveAshdiStreamsUseCase,
    private val resolveFenixStreams: ResolveFenixStreamsUseCase,
    private val messages: MikaiMessages,
) : ExtraStreamRepository {

    override fun supports(url: String): Boolean = isMikaiSourceUrl(url)

    override suspend fun resolve(url: String): PlayerStreamResolveResult {
        parseMikaiFenixSourceUrl(url)?.let { return resolveFenix(it) }
        val vodId = parseMikaiSourceUrl(url) ?: return PlayerStreamResolveResult.Failed("bad source url")
        return when (val result = resolveStreams(mikaiAshdiPageUrl(vodId))) {
            is AshdiStreamsResult.Success -> result.streams.toStream()
            AshdiStreamsResult.RegionBlocked -> PlayerStreamResolveResult.Unavailable(messages.regionBlocked)
            AshdiStreamsResult.NotFound -> PlayerStreamResolveResult.Unavailable(messages.notFound)
            is AshdiStreamsResult.Failed -> PlayerStreamResolveResult.Failed(result.reason)
        }
    }

    private suspend fun resolveFenix(token: String): PlayerStreamResolveResult =
        when (val result = resolveFenixStreams(mikaiFenixPageUrl(token))) {
            is FenixStreamsResult.Success -> PlayerStreamResolveResult.Stream(url = result.streams.masterUrl)
            FenixStreamsResult.NotFound -> PlayerStreamResolveResult.Unavailable(messages.notFound)
            is FenixStreamsResult.Failed -> PlayerStreamResolveResult.Failed(result.reason)
        }

    private fun AshdiStreams.toStream(): PlayerStreamResolveResult.Stream {
        if (qualities.isEmpty()) return PlayerStreamResolveResult.Stream(url = masterUrl)
        // Конвенция плеера: карта идёт по возрастанию качества, последнее считается лучшим.
        return PlayerStreamResolveResult.Stream(url = qualities.values.last(), qualities = LinkedHashMap(qualities))
    }
}
