package su.afk.yummy.tv.source.anistar.data.repository

import su.afk.yummy.tv.source.anistar.data.mapper.AnistarMapper
import su.afk.yummy.tv.source.anistar.data.network.AnistarApi
import su.afk.yummy.tv.source.anistar.data.utils.AnistarPlaylistEntry
import su.afk.yummy.tv.source.anistar.data.utils.parseAnistarPlaylist
import su.afk.yummy.tv.source.anistar.domain.model.AnistarEpisode
import su.afk.yummy.tv.source.anistar.domain.model.AnistarLookup
import su.afk.yummy.tv.source.anistar.domain.model.AnistarStreams
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTarget
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTitle
import su.afk.yummy.tv.source.anistar.domain.repository.AnistarRepository
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AnistarRepositoryImpl @Inject constructor(
    private val api: AnistarApi,
) : AnistarRepository {

    private data class CachedTitle(val title: AnistarTitle?, val fetchedAtMs: Long)

    private data class CachedPlaylist(val entries: List<AnistarPlaylistEntry>, val fetchedAtMs: Long)

    private data class CachedMirror(val mirror: String, val fetchedAtMs: Long)

    private val byMalId = ConcurrentHashMap<Int, CachedTitle>()
    private val playlists = ConcurrentHashMap<Long, CachedPlaylist>()

    @Volatile
    private var mirror: CachedMirror? = null

    override suspend fun findTitle(lookup: AnistarLookup): AnistarTitle? {
        val now = System.currentTimeMillis()
        byMalId[lookup.malId]?.takeIf { now - it.fetchedAtMs < TITLE_TTL_MS }?.let { return it.title }

        // Сбой сети в кэш не попадает: исключение уйдёт наверх, источник просто не появится в этот раз.
        val title = AnistarMapper.toTitle(api.findTitle(AnistarMapper.toRequest(lookup)))
        byMalId[lookup.malId] = CachedTitle(title, now)
        title?.let { mirror = CachedMirror(it.mirror, now) }
        return title
    }

    /** Список серий переживает [PLAYLIST_TTL_MS]: плейлист длинного тайтла — сотни килобайт, а экран деталей открывают часто. */
    override suspend fun getEpisodes(title: AnistarTitle): List<AnistarEpisode> {
        val now = System.currentTimeMillis()
        val entries = playlists[title.newsId]?.takeIf { now - it.fetchedAtMs < PLAYLIST_TTL_MS }?.entries
            ?: parseAnistarPlaylist(api.getPlayerPage(title.mirror, title.newsId)).also {
                playlists[title.newsId] = CachedPlaylist(it, now)
            }
        return entries.map { AnistarEpisode(it.number) }
    }

    /** Ссылки на HLS с токеном: плейлист берётся заново при каждом запуске серии. Зеркало, которое не отвечает, обновляется один раз. */
    override suspend fun getStreams(target: AnistarTarget): AnistarStreams? {
        val current = currentMirror(refresh = false)
        val (usedMirror, html) = try {
            current to api.getPlayerPage(current, target.newsId)
        } catch (_: IOException) {
            val fresh = currentMirror(refresh = true)
            fresh to api.getPlayerPage(fresh, target.newsId)
        }
        val entry = parseAnistarPlaylist(html).firstOrNull { it.number == target.episode } ?: return null
        return AnistarStreams(entry.hlsByQuality, referer = "$usedMirror/")
    }

    private suspend fun currentMirror(refresh: Boolean): String {
        val now = System.currentTimeMillis()
        mirror?.takeIf { !refresh && now - it.fetchedAtMs < MIRROR_TTL_MS }?.let { return it.mirror }
        val fresh = api.getMirror().mirror?.trim()?.trimEnd('/')?.takeIf { it.startsWith("https://") }
            ?: throw IOException("AniStar: сервер не вернул зеркало")
        mirror = CachedMirror(fresh, now)
        return fresh
    }

    private companion object {
        const val TITLE_TTL_MS = 10 * 60 * 1000L
        const val PLAYLIST_TTL_MS = 5 * 60 * 1000L
        const val MIRROR_TTL_MS = 30 * 60 * 1000L
    }
}
