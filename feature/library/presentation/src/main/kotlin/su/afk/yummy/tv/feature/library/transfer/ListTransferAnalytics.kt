package su.afk.yummy.tv.feature.library.transfer

import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.utils.analyticsParamsOf
import su.afk.yummy.tv.domain.library.model.AnimeListImportResult
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.domain.library.model.ImportConflictPolicy
import javax.inject.Inject

internal class ListTransferAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /** Пользователь открыл экран импорта и экспорта списка аниме. */
    fun eventScreenOpened() {
        tracker.track(EVENT_SCREEN_OPENED)
    }

    /**
     * Файл списка успешно разобран для предпросмотра.
     *
     * Параметры: format, total_count.
     */
    fun eventImportFileParsed(format: ExternalListFormat, totalCount: Int) {
        tracker.track(
            EVENT_IMPORT_FILE_PARSED,
            analyticsParamsOf(
                PARAM_FORMAT to format.name.lowercase(),
                PARAM_TOTAL_COUNT to totalCount,
            ),
        )
    }

    /**
     * Завершён (или остановлен) прогон импорта списка аниме.
     *
     * Параметры: format, conflict_policy, added, updated, skipped, unmatched, failed, cancelled.
     */
    fun eventImportFinished(
        format: ExternalListFormat,
        conflictPolicy: ImportConflictPolicy,
        result: AnimeListImportResult,
    ) {
        tracker.track(
            EVENT_IMPORT_FINISHED,
            analyticsParamsOf(
                PARAM_FORMAT to format.name.lowercase(),
                PARAM_CONFLICT_POLICY to conflictPolicy.name.lowercase(),
                PARAM_ADDED to result.addedCount,
                PARAM_UPDATED to result.updatedCount,
                PARAM_SKIPPED to result.skippedExistingCount,
                PARAM_UNMATCHED to result.unmatched.size,
                PARAM_FAILED to result.failedCount,
                PARAM_CANCELLED to result.wasCancelled,
            ),
        )
    }

    /**
     * Список аниме экспортирован в файл.
     *
     * Параметры: format, exported_count, skipped_no_id.
     */
    fun eventExportCompleted(
        format: ExternalListFormat,
        exportedCount: Int,
        skippedWithoutIdCount: Int,
    ) {
        tracker.track(
            EVENT_EXPORT_COMPLETED,
            analyticsParamsOf(
                PARAM_FORMAT to format.name.lowercase(),
                PARAM_EXPORTED_COUNT to exportedCount,
                PARAM_SKIPPED_NO_ID to skippedWithoutIdCount,
            ),
        )
    }

    private companion object {
        const val EVENT_SCREEN_OPENED = "list_transfer_opened"
        const val EVENT_IMPORT_FILE_PARSED = "list_transfer_import_parsed"
        const val EVENT_IMPORT_FINISHED = "list_transfer_import_finished"
        const val EVENT_EXPORT_COMPLETED = "list_transfer_export_completed"

        const val PARAM_FORMAT = "format"
        const val PARAM_CONFLICT_POLICY = "conflict_policy"
        const val PARAM_TOTAL_COUNT = "total_count"
        const val PARAM_ADDED = "added"
        const val PARAM_UPDATED = "updated"
        const val PARAM_SKIPPED = "skipped"
        const val PARAM_UNMATCHED = "unmatched"
        const val PARAM_FAILED = "failed"
        const val PARAM_CANCELLED = "cancelled"
        const val PARAM_EXPORTED_COUNT = "exported_count"
        const val PARAM_SKIPPED_NO_ID = "skipped_no_id"
    }
}
