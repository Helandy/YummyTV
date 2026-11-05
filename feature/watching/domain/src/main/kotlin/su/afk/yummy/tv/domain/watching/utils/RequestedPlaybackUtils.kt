package su.afk.yummy.tv.domain.watching.utils

import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.common.player.isSupportedPlayerUrl
import su.afk.yummy.tv.core.model.anime.AnimeVideo

/**
 * Выбирает видео под явный запрос (диплинк): серия сравнивается по [episodeGroupKey], балансер и
 * озвучка по вхождению без учёта регистра. Не заданное условие не ограничивает выбор; из подошедших
 * предпочитается то, чей плеер приложение умеет играть. Нет подходящего — null.
 */
internal fun List<AnimeVideo>.selectRequestedVideo(
    episode: String?,
    player: String?,
    dubbing: String?,
): AnimeVideo? {
    val matching = filter { video ->
        (episode.isNullOrBlank() || video.episode.episodeGroupKey() == episode.episodeGroupKey()) &&
            (player.isNullOrBlank() || video.player.contains(player, ignoreCase = true)) &&
            (dubbing.isNullOrBlank() || video.dubbing.contains(dubbing, ignoreCase = true))
    }
    return matching.firstOrNull { it.iframeUrl.isSupportedPlayerUrl() }
}
