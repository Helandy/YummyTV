package su.afk.yummy.tv.source.anidub.data.repository

import su.afk.yummy.tv.core.utils.network.BrowserUserAgentProvider
import su.afk.yummy.tv.source.anidub.data.network.AniDubApi
import su.afk.yummy.tv.source.anidub.data.utils.ANIDUB_BASE_URL
import su.afk.yummy.tv.source.anidub.data.utils.AniDubPlayerRef
import su.afk.yummy.tv.source.anidub.data.utils.aniDubEpisodeHash
import su.afk.yummy.tv.source.anidub.data.utils.aniDubHost
import su.afk.yummy.tv.source.anidub.data.utils.aniDubPageUrl
import su.afk.yummy.tv.source.anidub.data.utils.aniDubPlayer
import su.afk.yummy.tv.source.anidub.data.utils.aniDubQualities
import su.afk.yummy.tv.source.anidub.data.utils.aniDubReleaseHeight
import su.afk.yummy.tv.source.anidub.data.utils.isAniDubBadNode
import su.afk.yummy.tv.source.anidub.data.utils.masterUrl
import su.afk.yummy.tv.source.anidub.data.utils.playlistUrl
import su.afk.yummy.tv.source.anidub.domain.model.AniDubStreams
import su.afk.yummy.tv.source.anidub.domain.model.AniDubTarget
import su.afk.yummy.tv.source.anidub.domain.repository.AniDubRepository
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
internal class AniDubRepositoryImpl @Inject constructor(
    private val api: AniDubApi,
    private val userAgents: BrowserUserAgentProvider,
) : AniDubRepository {

    /** Состояние узла CDN по пробе; [alive] = `false` — узел не отдал плейлист. */
    private data class NodeState(val alive: Boolean, val untilMs: Long)

    private val nodes = ConcurrentHashMap<String, NodeState>()

    /**
     * Цепочка плеера: страница тайтла → встроенный плеер → плейлист серий → HLS-мастер серии. Ссылки на CDN подписаны
     * User-Agent, с которым запрошен мастер (с другим UA — 403), и живут около двух часов, поэтому всё читается в момент
     * воспроизведения одним UA, и он же уходит плееру в заголовках.
     */
    override suspend fun getStreams(target: AniDubTarget): AniDubStreams? {
        val ua = userAgents.userAgent
        val page = api.getText(aniDubPageUrl(target.slug), userAgent = ua) ?: return null
        // Тайтл без плеера (только Sibnet) приложением не играется: для него серии нет.
        val player = aniDubPlayer(page) ?: return null
        val playlist = try {
            api.getText(player.playlistUrl(), referer = "$ANIDUB_BASE_URL/", userAgent = ua)
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            null
        }
        val episodeHash = aniDubEpisodeHash(playlist, target.episode, player.hash) ?: return null
        return masterStreams(player, player.masterUrl(episodeHash), aniDubReleaseHeight(playlist), ua)
    }

    /**
     * Узлы CDN ротируются случайно на каждый запрос, и часть из них нерабочая, поэтому мастер переспрашивается (до
     * [MAX_ATTEMPTS] раз), пока не выпадет живой узел: известно плохие пропускаются, остальные проверяются пробой первого
     * варианта (в ответе должен быть `#EXTM3U`). Если живого не нашлось, отдаётся последний полученный вариант, лучше плохого
     * узла, чем ничего.
     */
    private suspend fun masterStreams(player: AniDubPlayerRef, masterUrl: String, releaseHeight: Int?, ua: String): AniDubStreams? {
        val headers = mapOf("User-Agent" to ua, "Accept" to "*/*", "Referer" to "${player.host}/", "Origin" to player.host)
        var last: AniDubStreams? = null
        var lastBad: AniDubStreams? = null
        repeat(MAX_ATTEMPTS) {
            val master = api.getText(masterUrl, referer = "${player.host}/", userAgent = ua) ?: return null
            val qualities = aniDubQualities(master, releaseHeight)
            if (qualities.isEmpty()) {
                // Мастер без вариантов: плеер играет его адаптивно, узел проверить нечем.
                if (master.startsWith("#EXTM3U")) return AniDubStreams(qualities, masterUrl, headers)
                return@repeat
            }
            val streams = AniDubStreams(qualities, masterUrl, headers)
            val node = aniDubHost(qualities.values.last())
            if (isAniDubBadNode(node)) {
                lastBad = streams
                return@repeat
            }
            last = streams
            when (nodeState(node)) {
                true -> return streams
                false -> return@repeat
                null -> if (probe(qualities.values.last(), player.host, ua)) {
                    nodes[node] = NodeState(true, System.currentTimeMillis() + NODE_TTL_MS)
                    return streams
                } else {
                    nodes[node] = NodeState(false, System.currentTimeMillis() + NODE_TTL_MS)
                }
            }
        }
        return last ?: lastBad
    }

    /** `true` / `false` — известное состояние узла, `null` — пробы ещё не было или она устарела. */
    private fun nodeState(node: String): Boolean? =
        nodes[node]?.takeIf { it.untilMs > System.currentTimeMillis() }?.alive

    private suspend fun probe(variantUrl: String, playerHost: String, ua: String): Boolean =
        try {
            api.getText(variantUrl, referer = "$playerHost/", userAgent = ua)?.contains("#EXTM3U") == true
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            false
        }

    private companion object {
        const val MAX_ATTEMPTS = 8
        const val NODE_TTL_MS = 20 * 60 * 1000L
    }
}
