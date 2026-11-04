package su.afk.yummy.tv.source.sameband.data.repository

import su.afk.yummy.tv.core.utils.network.BrowserUserAgentProvider
import su.afk.yummy.tv.source.sameband.data.mapper.toEpisodes
import su.afk.yummy.tv.source.sameband.data.model.SamebandEpisode
import su.afk.yummy.tv.source.sameband.data.network.SamebandApi
import su.afk.yummy.tv.source.sameband.data.utils.SAMEBAND_BASE_URL
import su.afk.yummy.tv.source.sameband.data.utils.parseSamebandPlaylist
import su.afk.yummy.tv.source.sameband.data.utils.samebandPageUrl
import su.afk.yummy.tv.source.sameband.data.utils.samebandPlayerUrl
import su.afk.yummy.tv.source.sameband.data.utils.samebandPlaylistUrl
import su.afk.yummy.tv.source.sameband.domain.model.SamebandStreams
import su.afk.yummy.tv.source.sameband.domain.model.SamebandTarget
import su.afk.yummy.tv.source.sameband.domain.repository.SamebandRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SamebandRepositoryImpl @Inject constructor(
    private val api: SamebandApi,
    private val userAgents: BrowserUserAgentProvider,
) : SamebandRepository {

    override suspend fun getEpisodes(slug: String): List<Int> = loadEpisodes(slug, userAgents.userAgent).map { it.number }.sorted()

    /**
     * Цепочка плеера: страница тайтла → iframe `/v/play/X.html` → `file:` в Playerjs → JSON `/v/list/X_list.txt`. Авторизации нет;
     * HLS отдаётся с Referer сайта, он же уходит плееру в заголовках.
     */
    override suspend fun getStreams(target: SamebandTarget): SamebandStreams? {
        val ua = userAgents.userAgent
        val episode = loadEpisodes(target.slug, ua).firstOrNull { it.number == target.episode } ?: return null
        val headers = mapOf("User-Agent" to ua, "Accept" to "*/*", "Referer" to "$SAMEBAND_BASE_URL/")
        return SamebandStreams(episode.qualities, headers)
    }

    private suspend fun loadEpisodes(slug: String, ua: String): List<SamebandEpisode> {
        val pageUrl = samebandPageUrl(slug)
        val page = api.getText(pageUrl, "$SAMEBAND_BASE_URL/", ua) ?: return emptyList()
        val playerUrl = samebandPlayerUrl(page) ?: return emptyList()
        val player = api.getText(playerUrl, pageUrl, ua) ?: return emptyList()
        val playlistUrl = samebandPlaylistUrl(player) ?: return emptyList()
        val playlist = api.getText(playlistUrl, playerUrl, ua) ?: return emptyList()
        return parseSamebandPlaylist(playlist).toEpisodes()
    }
}
