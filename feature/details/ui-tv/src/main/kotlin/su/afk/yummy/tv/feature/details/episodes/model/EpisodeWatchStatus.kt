package su.afk.yummy.tv.feature.details.episodes.model

internal sealed interface EpisodeWatchStatus {
    data object None : EpisodeWatchStatus

    data class InProgress(
        val progress: Float,
        val positionMs: Long,
        val durationMs: Long,
    ) : EpisodeWatchStatus

    data class Watched(
        val positionMs: Long,
        val durationMs: Long,
    ) : EpisodeWatchStatus
}
