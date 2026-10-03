package su.afk.yummy.tv.core.model.watching

/** Решение Continue Watching: что именно и с какого места запускать. */
data class ContinueWatchingLaunch(
    val animeId: Int,
    val animeTitle: String,
    val posterUrl: String,
    val video: ContinueWatchingPlaybackVideo,
    val resumeFromMs: Long,
    val remoteProgressSwitch: ContinueWatchingRemoteProgressSwitch? = null,
)
