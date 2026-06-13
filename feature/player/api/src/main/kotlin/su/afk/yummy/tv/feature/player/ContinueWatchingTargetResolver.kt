package su.afk.yummy.tv.feature.player

/** Player source selected for a continue-watching action and the source list to pass to player. */
data class ContinueWatchingTarget(
    val video: PlayerVideoSource,
    val allVideos: List<PlayerVideoSource>,
)

/** Resolves the best player source to resume playback from a stored progress entry. */
fun resolveContinueWatchingTarget(
    progressVideo: PlayerVideoSource,
    availableVideos: List<PlayerVideoSource>,
): ContinueWatchingTarget {
    val targetVideo = availableVideos.selectContinueWatchingVideo(
        videoId = progressVideo.id,
        episodeUrl = progressVideo.iframeUrl,
        episode = progressVideo.episode,
        playerName = progressVideo.player,
        dubbing = progressVideo.dubbing,
    ) ?: progressVideo
    val allVideos = availableVideos.ifEmpty { listOf(targetVideo) }
    return ContinueWatchingTarget(video = targetVideo, allVideos = allVideos)
}

/** Returns true when a placeholder progress episode can safely be migrated to the target episode. */
fun PlayerVideoSource.isTrustedPlaceholderMigrationTarget(targetVideo: PlayerVideoSource): Boolean {
    if (!episode.isPlaceholderEpisode() || targetVideo.episode.isPlaceholderEpisode()) return false
    if (episode == targetVideo.episode) return false
    return (id > 0 && targetVideo.id == id) ||
            (iframeUrl.isNotBlank() && targetVideo.iframeUrl == iframeUrl)
}
