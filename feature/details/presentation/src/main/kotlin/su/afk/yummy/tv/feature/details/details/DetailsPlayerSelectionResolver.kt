package su.afk.yummy.tv.feature.details.details

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.feature.details.utils.matchesPreferredPlayer
import su.afk.yummy.tv.feature.player.isSupportedPlayerUrl
import su.afk.yummy.tv.feature.player.playerDisplayOrderPriority

/** Resolves whether the selected episode can open directly or needs a balancer picker. */
internal fun resolveDetailsPlayerSelection(
    video: AnimeVideo,
    allVideos: List<AnimeVideo>,
    preferredPlayer: PreferredPlayer,
): DetailsPlayerSelection {
    val episodeVideos = allVideos.filter { it.episode == video.episode }
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
                options = options,
                preferredPlayerUnavailable = preferredPlayer != PreferredPlayer.NONE,
            )
        )
    } else {
        DetailsPlayerSelection.Navigate(targetVideo)
    }
}

/** UI action produced by details/episodes player source selection. */
internal sealed interface DetailsPlayerSelection {
    data class Navigate(val video: AnimeVideo) : DetailsPlayerSelection
    data class ShowPicker(val picker: BalancerPickerState) : DetailsPlayerSelection
}
