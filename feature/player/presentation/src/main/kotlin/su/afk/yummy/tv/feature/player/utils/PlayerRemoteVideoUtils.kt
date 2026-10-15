package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.feature.player.PlayerSourceEpisode
import su.afk.yummy.tv.feature.player.PlayerState

/**
 * Id видео, под которым прогресс принимает сервер Yani.
 *
 * У видео подключаемых источников (4K) id отрицательный и серверу неизвестен, поэтому ищем
 * настоящее видео той же серии: сперва в той же озвучке, затем в любой. `0` — аналога нет,
 * прогресс остаётся только локальным.
 */
internal fun PlayerState.State.remoteVideoId(): Int {
    val active = activeVideoId(this)
    if (active > 0) return active
    val episodeKey = activeEpisode(this).takeIf { it.isNotBlank() }?.episodeGroupKey() ?: return 0
    val dubbingName = activeDubbingName(this)
    val dubbings = sourceGraph.balancers.flatMap { it.dubbings }
    fun List<PlayerSourceEpisode>.match(): Int? =
        firstOrNull { it.id > 0 && it.number.episodeGroupKey() == episodeKey }?.id
    return dubbings.firstNotNullOfOrNull { dubbing ->
        dubbing.takeIf { it.name == dubbingName }?.episodes?.match()
    } ?: dubbings.firstNotNullOfOrNull { it.episodes.match() } ?: 0
}
