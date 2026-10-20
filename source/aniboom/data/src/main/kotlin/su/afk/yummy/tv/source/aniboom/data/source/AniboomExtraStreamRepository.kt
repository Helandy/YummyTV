package su.afk.yummy.tv.source.aniboom.data.source

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.aniboom.data.utils.isAniboomSourceUrl
import su.afk.yummy.tv.source.aniboom.data.utils.parseAniboomSourceUrl
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomStreamsResult
import su.afk.yummy.tv.source.aniboom.domain.usecase.ResolveAniboomStreamsUseCase
import javax.inject.Inject

/**
 * Превращает `source://aniboom/…` в HLS-поток. Заголовков плееру не нужно: CDN отдаёт плейлист без Referer и Origin
 * (с ними отвечает 400), а качество выбирается адаптивно по мастер-плейлисту.
 */
internal class AniboomExtraStreamRepository @Inject constructor(
    private val resolveStreams: ResolveAniboomStreamsUseCase,
    private val messages: AniboomMessages,
) : ExtraStreamRepository {

    override fun supports(url: String): Boolean = isAniboomSourceUrl(url)

    override suspend fun resolve(url: String): PlayerStreamResolveResult {
        val target = parseAniboomSourceUrl(url) ?: return PlayerStreamResolveResult.Failed("bad source url")
        return when (val result = resolveStreams(target)) {
            is AniboomStreamsResult.Success -> PlayerStreamResolveResult.Stream(url = result.streams.hlsUrl)
            AniboomStreamsResult.NotFound -> PlayerStreamResolveResult.Unavailable(messages.notFound)
            is AniboomStreamsResult.Failed -> PlayerStreamResolveResult.Failed(result.reason)
        }
    }
}
