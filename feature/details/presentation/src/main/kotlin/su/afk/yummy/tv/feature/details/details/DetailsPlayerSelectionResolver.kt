package su.afk.yummy.tv.feature.details.details

import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.utils.episode.episodeGroupKey
import su.afk.yummy.tv.feature.details.details.model.BalancerOption
import su.afk.yummy.tv.feature.details.details.model.BalancerPickerState
import su.afk.yummy.tv.feature.details.utils.dubbingEpisodeCount
import su.afk.yummy.tv.feature.details.utils.matchesPreferredPlayer
import su.afk.yummy.tv.feature.player.isSupportedPlayerUrl
import su.afk.yummy.tv.feature.player.playerDisplayOrderPriority

/** Resolves whether the selected episode can open directly or needs a balancer picker. */
internal fun resolveDetailsPlayerSelection(
    video: AnimeVideo,
    allVideos: List<AnimeVideo>,
    preferredPlayer: PreferredPlayer,
): DetailsPlayerSelection {
    val episodeVideos = allVideos.filter {
        it.episode.episodeGroupKey() == video.episode.episodeGroupKey()
    }
    // Если целевая озвучка в этом эпизоде есть только на неподдерживаемых балансерах,
    // пересаживаемся на самую популярную озвучку среди поддерживаемых.
    val targetVideo = if (
        episodeVideos.any { it.dubbing == video.dubbing && it.iframeUrl.isSupportedPlayerUrl() }
    ) {
        video
    } else {
        episodeVideos
            .filter { it.iframeUrl.isSupportedPlayerUrl() }
            .groupBy { it.dubbing }
            .maxByOrNull { (_, dubbingVideos) -> dubbingVideos.sumOf { it.views ?: 0 } }
            ?.value
            ?.maxByOrNull { it.views ?: 0 }
            ?: video
    }

    val options = episodeVideos
        .groupBy { it.player }
        .entries
        .map { (playerName, playerVideos) ->
            val representative = playerVideos.firstOrNull { it.dubbing == targetVideo.dubbing }
                ?: playerVideos.maxByOrNull { it.views ?: 0 }
                ?: playerVideos.first()
            BalancerOption(
                playerName = playerName,
                video = representative,
                isSupported = representative.iframeUrl.isSupportedPlayerUrl(),
                // Серии считаем по всему тайтлу: у балансера может быть двадцать серий,
                // хотя в пикер он попал из-за одной выбранной.
                episodeCount = allVideos
                    .filter {
                        it.player == playerName && it.dubbing == representative.dubbing
                    }
                    .dubbingEpisodeCount(),
            )
        }
        .sortedBy { option ->
            minOf(
                option.playerName.playerDisplayOrderPriority(),
                option.video.iframeUrl.playerDisplayOrderPriority(),
            )
        }
    val supportedOptions = options.filter { it.isSupported }

    if (preferredPlayer != PreferredPlayer.NONE) {
        val preferred = supportedOptions.firstOrNull {
            it.video.iframeUrl.matchesPreferredPlayer(preferredPlayer)
        }
        // Открываем напрямую только если у предпочитаемого балансера есть именно целевая
        // озвучка, иначе показываем пикер вместо тихой подмены озвучки.
        if (preferred != null && preferred.video.dubbing == targetVideo.dubbing) {
            return DetailsPlayerSelection.Navigate(preferred.video)
        }
    }

    return if (options.isNotEmpty()) {
        DetailsPlayerSelection.ShowPicker(
            BalancerPickerState(
                episodeNumber = video.episode,
                options = options.toImmutableList(),
                preferredPlayerUnavailable = preferredPlayer != PreferredPlayer.NONE,
            ),
        )
    } else {
        DetailsPlayerSelection.Navigate(targetVideo)
    }
}
