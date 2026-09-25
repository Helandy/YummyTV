package su.afk.yummy.tv.feature.details.utils

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.utils.episode.episodeGroupKey
import su.afk.yummy.tv.feature.player.isKodikPlayerUrl

/**
 * Просмотры озвучки: берём максимум по балансерам, а не сумму — один и тот же
 * сериал у разных плееров считается отдельно.
 */
internal fun List<AnimeVideo>.dubbingViews(): Int =
    groupBy { it.player }
        .values
        .maxOfOrNull { videos -> videos.sumOf { it.views ?: 0 } }
        ?: 0

/** Число серий озвучки по нормализованным номерам — см. [episodeGroupKey]. */
internal fun List<AnimeVideo>.dubbingEpisodeCount(): Int =
    map { it.episode.episodeGroupKey() }.distinct().size

/** Озвучка с наибольшим числом просмотров среди kodik-источников (или всех, если kodik нет). */
internal fun List<AnimeVideo>.mostWatchedDubbing(): String {
    val source = filter { it.iframeUrl.isKodikPlayerUrl() }.ifEmpty { this }
    return source.groupBy { it.dubbing }
        .maxByOrNull { (_, list) -> list.sumOf { it.views ?: 0 } }
        ?.key ?: source.firstOrNull()?.dubbing ?: ""
}
