package su.afk.yummy.tv.source.anime4k.data.source

import su.afk.yummy.tv.source.anime4k.data.flag.Anime4kFeatureGate
import su.afk.yummy.tv.source.anime4k.data.utils.isAnime4kSourceUrl
import su.afk.yummy.tv.source.anime4k.data.utils.parseAnime4kSourceUrl
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kStreams
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kStreamsResult
import su.afk.yummy.tv.source.anime4k.domain.usecase.ResolveAnime4kStreamsUseCase
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import javax.inject.Inject

/**
 * Превращает `source://anime4k/…` в прямой поток: реклама → тикет → ссылки. Вызывается при каждом открытии плеера;
 * повторный вход и смена качества в течение [REUSE_WINDOW_MS] используют уже выданный поток, чтобы не показывать
 * рекламу снова из-за перезагрузки.
 */
internal class Anime4kExtraStreamRepository @Inject constructor(
    private val resolveStreams: ResolveAnime4kStreamsUseCase,
    private val featureGate: Anime4kFeatureGate,
    private val messages: Anime4kMessages,
) : ExtraStreamRepository {

    private data class Granted(val url: String, val result: PlayerStreamResolveResult.Stream, val atMs: Long)

    @Volatile
    private var last: Granted? = null

    /**
     * Плеер сразу делает один тихий повтор после неудачи: отказ пользователя не должен вызывать вопрос второй раз.
     * Запись одноразовая — следующий (ручной) повтор спрашивает заново.
     */
    @Volatile
    private var declined: Pair<String, Long>? = null

    override fun supports(url: String): Boolean =
        isAnime4kSourceUrl(url) && featureGate.isEnabled()

    override suspend fun resolve(url: String): PlayerStreamResolveResult {
        val now = System.currentTimeMillis()
        last?.takeIf { it.url == url && now - it.atMs < REUSE_WINDOW_MS }?.let { return it.result }

        declined?.takeIf { it.first == url && now - it.second < DECLINE_QUIET_MS }?.let {
            // Отказ гасит только один тихий повтор; ручное «Повторить» снова задаст вопрос.
            declined = null
            return PlayerStreamResolveResult.Unavailable(messages.declined)
        }

        val target = parseAnime4kSourceUrl(url) ?: return PlayerStreamResolveResult.Failed("bad source url")
        return when (val result = resolveStreams(target)) {
            is Anime4kStreamsResult.Success -> result.streams.toStream().also {
                last = Granted(url, it, System.currentTimeMillis())
            }
            Anime4kStreamsResult.Declined -> {
                declined = url to System.currentTimeMillis()
                PlayerStreamResolveResult.Unavailable(messages.declined)
            }
            Anime4kStreamsResult.AdNotRewarded -> PlayerStreamResolveResult.Unavailable(messages.adNotShown)
            Anime4kStreamsResult.RateLimited -> PlayerStreamResolveResult.Unavailable(messages.rateLimited)
            Anime4kStreamsResult.Rejected -> PlayerStreamResolveResult.Failed("ticket rejected")
            Anime4kStreamsResult.NotFound -> PlayerStreamResolveResult.Unavailable(messages.notFound)
            is Anime4kStreamsResult.Failed -> PlayerStreamResolveResult.Failed(result.reason)
        }
    }

    private fun Anime4kStreams.toStream(): PlayerStreamResolveResult.Stream {
        val headers = mapOf("Referer" to referer)
        // Конвенция плеера (как у Kodik и Aksor): карта идёт по возрастанию качества, последнее считается лучшим.
        val ascending = files.sortedBy { it.quality }
        val qualities = LinkedHashMap<String, String>()
        ascending.forEach { qualities["${it.quality}p"] = it.url }
        return PlayerStreamResolveResult.Stream(
            url = ascending.last().url,
            headers = headers,
            qualities = qualities,
        )
    }

    private companion object {
        const val REUSE_WINDOW_MS = 60_000L
        const val DECLINE_QUIET_MS = 10_000L
    }
}
