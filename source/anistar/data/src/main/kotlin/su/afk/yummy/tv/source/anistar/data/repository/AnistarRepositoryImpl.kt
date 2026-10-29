package su.afk.yummy.tv.source.anistar.data.repository

import su.afk.yummy.tv.source.anistar.data.network.AnistarApi
import su.afk.yummy.tv.source.anistar.data.utils.parseAnistarPlaylist
import su.afk.yummy.tv.source.anistar.domain.model.AnistarStreams
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTarget
import su.afk.yummy.tv.source.anistar.domain.repository.AnistarRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AnistarRepositoryImpl @Inject constructor(
    private val api: AnistarApi,
) : AnistarRepository {

    private data class CachedMirror(val mirror: String, val fetchedAtMs: Long)

    @Volatile
    private var mirror: CachedMirror? = null

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
        const val MIRROR_TTL_MS = 30 * 60 * 1000L
    }
}
