package su.afk.yummy.tv.feature.player

/** Player source selected for a continue-watching action. */
data class ContinueWatchingTarget(
    val video: PlayerVideoSource,
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
    return ContinueWatchingTarget(video = targetVideo)
}
