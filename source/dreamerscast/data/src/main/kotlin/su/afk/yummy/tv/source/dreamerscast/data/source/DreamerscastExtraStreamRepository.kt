package su.afk.yummy.tv.source.dreamerscast.data.source

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.dreamerscast.data.utils.isDreamerscastSourceUrl
import su.afk.yummy.tv.source.dreamerscast.data.utils.parseDreamerscastSourceUrl
import su.afk.yummy.tv.source.dreamerscast.domain.model.DreamerscastStreams
import su.afk.yummy.tv.source.dreamerscast.domain.model.DreamerscastStreamsResult
import su.afk.yummy.tv.source.dreamerscast.domain.usecase.ResolveDreamerscastStreamsUseCase
import javax.inject.Inject

/**
 * Превращает `source://dreamerscast/…` в HLS-поток. Заголовков плееру не нужно. Мастер разобран на варианты: отдаётся карта качеств
 * по возрастанию (лучшее по умолчанию — последнее), иначе мастер играет адаптивно («Авто»).
 */
internal class DreamerscastExtraStreamRepository @Inject constructor(
    private val resolveStreams: ResolveDreamerscastStreamsUseCase,
    private val messages: DreamerscastMessages,
) : ExtraStreamRepository {

    override fun supports(url: String): Boolean = isDreamerscastSourceUrl(url)

    override suspend fun resolve(url: String): PlayerStreamResolveResult {
        val target = parseDreamerscastSourceUrl(url) ?: return PlayerStreamResolveResult.Failed("bad source url")
        return when (val result = resolveStreams(target)) {
            is DreamerscastStreamsResult.Success -> result.streams.toStream()
            DreamerscastStreamsResult.NotFound -> PlayerStreamResolveResult.Unavailable(messages.notFound)
            is DreamerscastStreamsResult.Failed -> PlayerStreamResolveResult.Failed(result.reason)
        }
    }

    private fun DreamerscastStreams.toStream(): PlayerStreamResolveResult.Stream {
        if (qualities.isEmpty()) return PlayerStreamResolveResult.Stream(url = masterUrl)
        // Конвенция плеера: карта идёт по возрастанию качества, последнее считается лучшим.
        return PlayerStreamResolveResult.Stream(url = qualities.values.last(), qualities = LinkedHashMap(qualities))
    }
}
