package su.afk.yummy.tv.data.videodownload.worker

import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import javax.inject.Inject

internal class VideoDownloadAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    fun reportSucceeded(item: VideoDownloadItem) {
        tracker.track(EVENT_SUCCEEDED, item.analyticsParams())
    }

    fun reportFailed(
        item: VideoDownloadItem,
        details: String,
        throwable: Throwable? = null,
    ) {
        val message = buildString {
            append("Video download failed (")
            append(item.analyticsParams().entries.joinToString { (key, value) -> "$key=$value" })
            append("): ")
            append(details)
        }
        tracker.reportError(
            message = message,
            throwable = throwable ?: IllegalStateException(details),
            groupIdentifier = ERROR_GROUP,
        )
    }

    private fun VideoDownloadItem.analyticsParams(): Map<String, String> = analyticsParamsOf(
        PARAM_DOWNLOAD_ID to id,
        PARAM_ANIME_ID to animeId,
        PARAM_ANIME_TITLE to animeTitle,
        PARAM_EPISODE to episode,
        PARAM_DUBBING to dubbing,
        PARAM_PLAYER to playerName,
        PARAM_PLAYER_ID to playerId,
        PARAM_QUALITY to qualityLabel,
    )

    private companion object {
        const val EVENT_SUCCEEDED = "video_download_succeeded"
        const val ERROR_GROUP = "video_download_failed"
        const val PARAM_DOWNLOAD_ID = "download_id"
        const val PARAM_ANIME_ID = "anime_id"
        const val PARAM_ANIME_TITLE = "anime_title"
        const val PARAM_EPISODE = "episode"
        const val PARAM_DUBBING = "dubbing"
        const val PARAM_PLAYER = "player"
        const val PARAM_PLAYER_ID = "player_id"
        const val PARAM_QUALITY = "quality"
    }
}
