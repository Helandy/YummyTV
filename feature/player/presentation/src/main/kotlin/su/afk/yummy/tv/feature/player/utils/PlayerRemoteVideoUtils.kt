package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.feature.player.PlayerSourceEpisode
import su.afk.yummy.tv.feature.player.PlayerState
import kotlin.random.Random

/** Случайная соль процесса: выбор видео стабилен на время сессии, но различается между запусками. */
private val processPickSeed = Random.nextInt()

/**
 * Id видео, под которым прогресс принимает сервер Yani.
 *
 * У видео подключаемых источников (4K) id отрицательный и серверу неизвестен, поэтому ищем
 * настоящее видео той же серии: сперва в той же озвучке, затем в любой. Из нескольких кандидатов
 * берём случайного, чтобы не перекашивать статистику выбора плееров у Yani; выбор стабилен на время
 * сессии ([pickSeed]), иначе секунды просмотра разлетелись бы по разным видео. `0` — аналога нет,
 * прогресс остаётся только локальным.
 */
internal fun PlayerState.State.remoteVideoId(pickSeed: Int = processPickSeed): Int {
    val active = activeVideoId(this)
    if (active > 0) return active
    val episodeKey = activeEpisode(this).takeIf { it.isNotBlank() }?.episodeGroupKey() ?: return 0
    val dubbingName = activeDubbingName(this)
    val dubbings = sourceGraph.balancers.flatMap { it.dubbings }
    fun List<PlayerSourceEpisode>.matches(): List<Int> =
        filter { it.id > 0 && it.number.episodeGroupKey() == episodeKey }.map { it.id }

    val sameDubbing = dubbings.filter { it.name == dubbingName }.flatMap { it.episodes.matches() }
    val candidates = sameDubbing.ifEmpty { dubbings.flatMap { it.episodes.matches() } }.distinct()
    if (candidates.isEmpty()) return 0
    return candidates[Math.floorMod(listOf(pickSeed, episodeKey, dubbingName).hashCode(), candidates.size)]
}
