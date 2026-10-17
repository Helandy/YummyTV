package su.afk.yummy.tv.feature.details.mapper

import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.common.player.isSupportedPlayerUrl
import su.afk.yummy.tv.core.common.player.balancerDisplayComparator
import su.afk.yummy.tv.core.common.player.withoutPlayerPrefix
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.feature.details.episodes.dubbings.EpisodeDubbingsState
import su.afk.yummy.tv.feature.details.utils.dubbingEpisodeCount
import su.afk.yummy.tv.feature.details.utils.dubbingViews

internal fun List<AnimeVideo>.episodeDubbingItems(
    episode: String,
): List<EpisodeDubbingsState.DubbingItem> {
    val viewsByPlayer = groupingBy { it.player }.fold(0) { sum, video -> sum + (video.views ?: 0) }
    val videosByDubbing = asSequence()
        .mapNotNull { video ->
            video.dubbing.trim().takeIf { it.isNotBlank() }?.let { it to video }
        }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
    val episodeVideosByDubbing = asSequence()
        .filter { it.episode.episodeGroupKey() == episode.episodeGroupKey() }
        .mapNotNull { video ->
            video.dubbing.trim().takeIf { it.isNotBlank() }?.let { it to video }
        }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
    return episodeVideosByDubbing.keys
        .map { dubbing ->
            val dubbingVideos = videosByDubbing[dubbing].orEmpty()
            EpisodeDubbingsState.DubbingItem(
                name = dubbing,
                views = dubbingVideos.dubbingViews(),
                episodeCount = dubbingVideos.dubbingEpisodeCount(),
                // Балансеры считаем только по выбранной серии: у озвучки может быть пять
                // плееров на сериал и один на конкретную серию — иначе карточка врёт.
                supportedBalancers = episodeVideosByDubbing[dubbing]
                    .orEmpty()
                    .supportedBalancersLabel(viewsByPlayer),
            )
        }
        .sortedWith(
            compareByDescending<EpisodeDubbingsState.DubbingItem> { it.views }
                .thenBy { it.name }
        )
        .toList()
}

private fun List<AnimeVideo>.supportedBalancersLabel(viewsByPlayer: Map<String, Int>): String =
    asSequence()
        .filter { it.iframeUrl.isSupportedPlayerUrl() }
        .distinctBy { it.player }
        .sortedWith(
            balancerDisplayComparator<AnimeVideo>(
                playerName = { it.player },
                iframeUrl = { it.iframeUrl },
                views = { video -> viewsByPlayer[video.player] ?: 0 },
            ),
        )
        .map { it.player.withoutPlayerPrefix() }
        .joinToString(" • ")
