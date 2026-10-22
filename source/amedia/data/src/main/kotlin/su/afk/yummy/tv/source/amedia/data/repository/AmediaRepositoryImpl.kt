package su.afk.yummy.tv.source.amedia.data.repository

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.amedia.data.network.AmediaApi
import su.afk.yummy.tv.source.amedia.data.utils.parseAmediaEpisodes
import su.afk.yummy.tv.source.amedia.data.utils.parseAmediaMalId
import su.afk.yummy.tv.source.amedia.data.utils.parseAmediaMasterPlaylist
import su.afk.yummy.tv.source.amedia.data.utils.parseAmediaSearchLinks
import su.afk.yummy.tv.source.amedia.data.utils.parseAmediaVodFile
import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisode
import su.afk.yummy.tv.source.amedia.domain.model.AmediaLookup
import su.afk.yummy.tv.source.amedia.domain.model.AmediaStreams
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTarget
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTitle
import su.afk.yummy.tv.source.amedia.domain.repository.AmediaRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AmediaRepositoryImpl @Inject constructor(
    private val api: AmediaApi,
) : AmediaRepository {

    private data class CachedTitle(val title: AmediaTitle?, val fetchedAtMs: Long)

    private data class CachedEpisodes(val episodes: List<AmediaEpisode>, val fetchedAtMs: Long)

    private val byMalId = ConcurrentHashMap<Int, CachedTitle>()
    private val episodes = ConcurrentHashMap<String, CachedEpisodes>()

    /**
     * Сайт ищет только по русским названиям, поэтому в запросы идут все названия тайтла и их часть до двоеточия. Страница
     * принимается лишь с совпавшим MAL id: похожих названий у сезонов много.
     */
    override suspend fun findTitle(lookup: AmediaLookup): AmediaTitle? {
        val now = System.currentTimeMillis()
        byMalId[lookup.malId]?.takeIf { now - it.fetchedAtMs < TITLE_TTL_MS }?.let { return it.title }

        val checked = HashSet<String>()
        var found: AmediaTitle? = null
        search@ for (query in queries(lookup)) {
            for (link in parseAmediaSearchLinks(api.search(query))) {
                if (checked.size >= MAX_PAGES) break@search
                if (!checked.add(link)) continue
                val html = api.getTitlePage(link)
                if (parseAmediaMalId(html) == lookup.malId) {
                    episodes[link] = CachedEpisodes(parseAmediaEpisodes(html), now)
                    found = AmediaTitle(link)
                    break@search
                }
            }
        }
        // Сбой сети в кэш не попадает: исключение уйдёт наверх, источник просто не появится в этот раз.
        byMalId[lookup.malId] = CachedTitle(found, now)
        return found
    }

    override suspend fun getEpisodes(title: AmediaTitle): List<AmediaEpisode> {
        val now = System.currentTimeMillis()
        episodes[title.pageUrl]?.takeIf { now - it.fetchedAtMs < EPISODES_TTL_MS }?.let { return it.episodes }
        return parseAmediaEpisodes(api.getTitlePage(title.pageUrl)).also { episodes[title.pageUrl] = CachedEpisodes(it, now) }
    }

    /** Мастер-плейлист не подписан, но берётся заново при каждом запуске серии; недоступные варианты качества не мешают воспроизведению. */
    override suspend fun getStreams(target: AmediaTarget): AmediaStreams? {
        val masterUrl = parseAmediaVodFile(api.getVodPage(target.vodId)) ?: return null
        val variants = try {
            parseAmediaMasterPlaylist(api.getPlaylist(masterUrl), masterUrl)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyMap()
        }
        return AmediaStreams(masterUrl, variants)
    }

    private fun queries(lookup: AmediaLookup): List<String> =
        (listOf(lookup.title) + lookup.otherTitles)
            .flatMap { listOf(it, it.substringBefore(':')) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_QUERIES)

    private companion object {
        const val TITLE_TTL_MS = 10 * 60 * 1000L
        const val EPISODES_TTL_MS = 5 * 60 * 1000L
        const val MAX_QUERIES = 4
        const val MAX_PAGES = 6
    }
}
