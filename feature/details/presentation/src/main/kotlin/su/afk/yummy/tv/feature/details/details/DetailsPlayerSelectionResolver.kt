package su.afk.yummy.tv.feature.details.details

import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.utils.matchesPreferredPlayer
import su.afk.yummy.tv.feature.player.isSupportedPlayerUrl
import su.afk.yummy.tv.feature.player.playerDisplayOrderPriority

/** Resolves whether the selected episode can open directly or needs a balancer picker. */
internal fun resolveDetailsPlayerSelection(
    video: AnimeVideo,
    allVideos: List<AnimeVideo>,
    preferredPlayer: PreferredPlayer,
): DetailsPlayerSelection {
    val options = allVideos
        .filter { it.episode == video.episode }
        .groupBy { it.player }
        .entries
        .map { (playerName, playerVideos) ->
            val supported = playerVideos.first().iframeUrl.isSupportedPlayerUrl()
            val representative = playerVideos.firstOrNull { it.dubbing == video.dubbing }
                ?: playerVideos.maxByOrNull { it.views ?: 0 }
                ?: playerVideos.first()
            BalancerOption(
                playerName = playerName,
                video = representative,
                isSupported = supported,
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
        if (preferred != null) return DetailsPlayerSelection.Navigate(preferred.video)
    }

    return when (supportedOptions.size) {
        0 -> DetailsPlayerSelection.Navigate(video)
        1 -> DetailsPlayerSelection.Navigate(supportedOptions.first().video)
        else -> DetailsPlayerSelection.ShowPicker(BalancerPickerState(video.episode, options))
    }
}

/** UI action produced by details/episodes player source selection. */
internal sealed interface DetailsPlayerSelection {
    data class Navigate(val video: AnimeVideo) : DetailsPlayerSelection
    data class ShowPicker(val picker: BalancerPickerState) : DetailsPlayerSelection
}
