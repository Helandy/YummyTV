package su.afk.yummy.tv.feature.details.episodes.dubbings

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.feature.details.utils.matchesPreferredPlayer
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
    return asSequence()
        .filter { it.episode == episode }
        .map { it.dubbing.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .map { dubbing ->
            val dubbingVideos = videosByDubbing[dubbing].orEmpty()
            EpisodeDubbingsState.DubbingItem(
                name = dubbing,
                views = dubbingVideos.dubbingViews(),
                episodeCount = dubbingVideos.dubbingEpisodeCount(),
                supportedBalancers = dubbingVideos.supportedBalancersLabel(),
            )
        }
        .sortedWith(
            compareByDescending<EpisodeDubbingsState.DubbingItem> { it.views }
                .thenBy { it.name }
        )
        .toList()
}

internal fun List<AnimeVideo>.selectEpisodeDubbingLaunchVideo(
    episode: String,
    dubbingName: String,
    preferredPlayer: PreferredPlayer,
): AnimeVideo? {
    val candidates = filter { it.episode == episode && it.dubbing.trim() == dubbingName }
    val supported = candidates.filter { it.iframeUrl.isSupportedPlayerUrl() }
    return supported.firstOrNull { it.iframeUrl.matchesPreferredPlayer(preferredPlayer) }
        ?: supported.minWithOrNull(
            compareBy<AnimeVideo> {
                minOf(
                    it.player.playerDisplayOrderPriority(),
                    it.iframeUrl.playerDisplayOrderPriority(),
                )
            }.thenBy { it.player }
        )
        ?: candidates.firstOrNull()
}

private fun List<AnimeVideo>.dubbingViews(): Int =
    groupBy { it.player }
        .values
        .maxOfOrNull { videos -> videos.sumOf { it.views ?: 0 } }
        ?: 0

private fun List<AnimeVideo>.dubbingEpisodeCount(): Int = map { it.episode }.distinct().size

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
