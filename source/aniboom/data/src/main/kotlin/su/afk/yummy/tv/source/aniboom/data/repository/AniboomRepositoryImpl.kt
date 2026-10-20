package su.afk.yummy.tv.source.aniboom.data.repository

import su.afk.yummy.tv.source.aniboom.data.mapper.AniboomMapper
import su.afk.yummy.tv.source.aniboom.data.network.AniboomApi
import su.afk.yummy.tv.source.aniboom.data.utils.ANIBOOM_REFERER
import su.afk.yummy.tv.source.aniboom.data.utils.aniboomEmbedUrl
import su.afk.yummy.tv.source.aniboom.data.utils.parseAniboomHlsUrl
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomLookup
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomStreams
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTarget
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTitle
import su.afk.yummy.tv.source.aniboom.domain.repository.AniboomRepository
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AniboomRepositoryImpl @Inject constructor(
    private val api: AniboomApi,
) : AniboomRepository {

    private data class Cached(val title: AniboomTitle?, val fetchedAtMs: Long)

    private data class CachedEpisode(val exists: Boolean, val checkedAtMs: Long)

    private val byMalId = ConcurrentHashMap<Int, Cached>()
    private val episodeChecks = ConcurrentHashMap<AniboomTarget, CachedEpisode>()

    override suspend fun findTitle(lookup: AniboomLookup): AniboomTitle? {
        val now = System.currentTimeMillis()
        byMalId[lookup.malId]?.takeIf { now - it.fetchedAtMs < TTL_MS }?.let { return it.title }

        // Сбой сети в кэш не попадает: исключение уйдёт наверх, источник просто не появится в этот раз.
        val title = AniboomMapper.toTitle(api.findTitle(AniboomMapper.toRequest(lookup)))
        byMalId[lookup.malId] = Cached(title, now)
        return title
    }

    override suspend fun getStreams(target: AniboomTarget): AniboomStreams? {
        val html = api.getEmbed(aniboomEmbedUrl(target.aniboomId, target.episode, target.translation), ANIBOOM_REFERER)
            ?: return null
        val hls = parseAniboomHlsUrl(html) ?: throw IOException("Aniboom embed: нет ссылки на HLS")
        return AniboomStreams(hls)
    }

    /** 404 у эмбеда — серия-заглушка. Ответ живёт [EPISODE_TTL_MS]: вышедшая серия появится в приложении не позже чем через это время. */
    override suspend fun hasEpisode(target: AniboomTarget): Boolean {
        val now = System.currentTimeMillis()
        episodeChecks[target]?.takeIf { now - it.checkedAtMs < EPISODE_TTL_MS }?.let { return it.exists }

        val exists = api.getEmbed(aniboomEmbedUrl(target.aniboomId, target.episode, target.translation), ANIBOOM_REFERER) != null
        episodeChecks[target] = CachedEpisode(exists, now)
        return exists
    }

    private companion object {
        const val TTL_MS = 10 * 60 * 1000L
        const val EPISODE_TTL_MS = 5 * 60 * 1000L
    }
}
