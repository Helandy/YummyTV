package su.afk.yummy.tv.feature.details.mobile.episodes.model

internal sealed interface EpisodeMobileWatchStatus {
    data object None : EpisodeMobileWatchStatus

    data class InProgress(
        val progress: Float,
        val positionMs: Long,
        val durationMs: Long,
    ) : EpisodeMobileWatchStatus

    data class Watched(
        val positionMs: Long,
        val durationMs: Long,
    ) : EpisodeMobileWatchStatus
}
