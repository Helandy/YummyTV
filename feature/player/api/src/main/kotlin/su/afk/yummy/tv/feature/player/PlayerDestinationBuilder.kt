package su.afk.yummy.tv.feature.player

import androidx.navigation3.runtime.NavKey

data class PlayerVideoSource(
    val id: Int,
    val episode: String,
    val dubbing: String,
    val player: String,
    val playerId: Int? = null,
    val iframeUrl: String,
    val views: Int? = null,
    val skips: PlayerSkips = PlayerSkips.Empty,
)

fun IPlayerNavigator.getPlayerDest(
    video: PlayerVideoSource,
    animeTitle: String,
    animeId: Int,
    posterUrl: String = "",
    screenshotByEpisode: Map<String, String> = emptyMap(),
): NavKey {
    return getPlayerDest(
        iframeUrl = video.iframeUrl,
        animeTitle = animeTitle,
        episode = video.episode,
        playerName = video.player,
        dubbing = video.dubbing,
        selectedVideoId = video.id,
        selectedPlayerId = video.playerId,
        selectedScreenshotUrl = video.iframeUrl
            .takeIf { it.isKodikPlayerUrl() }
            ?: screenshotByEpisode[video.episode].orEmpty(),
        animeId = animeId,
        posterUrl = posterUrl,
    )
}

fun List<PlayerVideoSource>.selectContinueWatchingVideo(
    videoId: Int,
    episodeUrl: String,
    episode: String,
    playerName: String,
    dubbing: String,
): PlayerVideoSource? {
    val exact = firstOrNull { videoId > 0 && it.id == videoId }
        ?: firstOrNull { episodeUrl.isNotBlank() && it.iframeUrl == episodeUrl }
        ?: firstOrNull {
            !episode.isPlaceholderEpisode() &&
                it.episode == episode &&
                it.player == playerName &&
                it.dubbing == dubbing
        }
        ?: firstOrNull { !episode.isPlaceholderEpisode() && it.episode == episode }

    if (exact?.iframeUrl?.isSupportedPlayerUrl() == true) return exact

    val targetEpisode = exact?.episode?.takeUnless { it.isPlaceholderEpisode() }
        ?: episode.takeUnless { it.isPlaceholderEpisode() }
    val sameEpisode = targetEpisode?.let { ep -> filter { it.episode == ep } }.orEmpty()
    return sameEpisode.firstOrNull { it.iframeUrl.isSupportedPlayerUrl() }
        ?: firstOrNull { it.iframeUrl.isSupportedPlayerUrl() }
        ?: exact
}

fun String.isPlaceholderEpisode(): Boolean = trim().isEmpty() || trim() == "-"
