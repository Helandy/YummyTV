package su.afk.yummy.tv.data.videodownload.worker

import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoExportSource
import javax.inject.Inject

/** Воронка экспорта: выбор папки → постановка в очередь → результат. */
class VideoExportAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /** Пользователь выбрал папку и она пригодна для записи. */
    fun reportDirectorySelected() {
        tracker.track(EVENT_DIRECTORY_SELECTED)
    }

    /** Выбранная папка не подошла: пригодится, чтобы отличить отказ провайдера от отказа юзера. */
    fun reportDirectoryRejected(reason: String) {
        tracker.track(EVENT_DIRECTORY_REJECTED, analyticsParamsOf(PARAM_REASON to reason))
    }

    /**
     * Экспорт поставлен в очередь: вручную из списка загрузок или автоматически после скачивания.
     * Для одиночного экспорта добавляем параметры серии — у массового они бессмысленны.
     */
    fun reportEnqueued(count: Int, source: VideoExportSource, item: VideoDownloadItem?) {
        val itemParams = if (count == 1) item?.analyticsParams().orEmpty() else emptyMap()
        tracker.track(
            EVENT_ENQUEUED,
            analyticsParamsOf(
                PARAM_COUNT to count,
                PARAM_SOURCE to source.name.lowercase(),
            ) + itemParams,
        )
    }

    fun reportSucceeded(item: VideoDownloadItem, durationMs: Long, bytes: Long) {
        tracker.track(
            EVENT_SUCCEEDED,
            item.analyticsParams() + analyticsParamsOf(
                PARAM_DURATION_MS to durationMs,
                PARAM_BYTES to bytes,
            ),
        )
    }

    fun reportFailed(
        item: VideoDownloadItem,
        details: String,
        throwable: Throwable? = null,
    ) {
        val message = buildString {
            append("Video export failed (")
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

    fun reportCancelled() {
        tracker.track(EVENT_CANCELLED)
    }

    private fun VideoDownloadItem.analyticsParams(): Map<String, String> = analyticsParamsOf(
        PARAM_DOWNLOAD_ID to id,
        PARAM_ANIME_ID to animeId,
        PARAM_ANIME_TITLE to animeTitle,
        PARAM_EPISODE to episode,
        PARAM_DUBBING to dubbing,
        PARAM_PLAYER to playerName,
        PARAM_QUALITY to qualityLabel,
    )

    private companion object {
        const val EVENT_DIRECTORY_SELECTED = "video_export_directory_selected"
        const val EVENT_DIRECTORY_REJECTED = "video_export_directory_rejected"
        const val EVENT_ENQUEUED = "video_export_enqueued"
        const val EVENT_SUCCEEDED = "video_export_succeeded"
        const val EVENT_CANCELLED = "video_export_cancelled"
        const val ERROR_GROUP = "video_export_failed"
        const val PARAM_REASON = "reason"
        const val PARAM_COUNT = "count"
        const val PARAM_SOURCE = "source"
        const val PARAM_DURATION_MS = "duration_ms"
        const val PARAM_BYTES = "bytes"
        const val PARAM_DOWNLOAD_ID = "download_id"
        const val PARAM_ANIME_ID = "anime_id"
        const val PARAM_ANIME_TITLE = "anime_title"
        const val PARAM_EPISODE = "episode"
        const val PARAM_DUBBING = "dubbing"
        const val PARAM_PLAYER = "player"
        const val PARAM_QUALITY = "quality"
    }
}
