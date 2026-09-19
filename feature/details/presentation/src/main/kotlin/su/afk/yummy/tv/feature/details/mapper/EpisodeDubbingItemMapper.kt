package su.afk.yummy.tv.feature.details.mapper

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.utils.episode.episodeGroupKey
import su.afk.yummy.tv.feature.details.episodes.dubbings.EpisodeDubbingsState
import su.afk.yummy.tv.feature.details.utils.dubbingEpisodeCount
import su.afk.yummy.tv.feature.details.utils.dubbingViews
import su.afk.yummy.tv.feature.player.isSupportedPlayerUrl
import su.afk.yummy.tv.feature.player.playerDisplayOrderPriority

internal fun List<AnimeVideo>.episodeDubbingItems(
    episode: String,
): List<EpisodeDubbingsState.DubbingItem> {
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
                    .supportedBalancersLabel(),
            )
        }
        .sortedWith(
            compareByDescending<EpisodeDubbingsState.DubbingItem> { it.views }
                .thenBy { it.name }
        )
        .toList()
}

private fun List<AnimeVideo>.supportedBalancersLabel(): String =
    asSequence()
        .filter { it.iframeUrl.isSupportedPlayerUrl() }
        .distinctBy { it.player }
        .sortedWith(
            compareBy<AnimeVideo> {
                minOf(
                    it.player.playerDisplayOrderPriority(),
                    it.iframeUrl.playerDisplayOrderPriority(),
                )
            }.thenBy { it.player }
        )
        .map { it.player.removePrefix(RU_PLAYER_PREFIX).removePrefix(EN_PLAYER_PREFIX) }
        .joinToString(" • ")

private const val RU_PLAYER_PREFIX = "Плеер "
private const val EN_PLAYER_PREFIX = "Player "
