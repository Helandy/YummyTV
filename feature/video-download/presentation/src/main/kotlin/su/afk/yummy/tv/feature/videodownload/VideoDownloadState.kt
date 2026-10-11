package su.afk.yummy.tv.feature.videodownload

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.model.VideoExportDestination

class VideoDownloadState {
    @Immutable
    data class State(
        val items: ImmutableList<VideoDownloadItem> = persistentListOf(),
        val pendingDeleteItem: VideoDownloadItem? = null,
        val pendingReExportItem: VideoDownloadItem? = null,
        val exportDestination: VideoExportDestination? = null,
        val pendingBulkExportCount: Int = 0,
        /** Системный запрос разрешения на уведомления уже показывали хотя бы раз. */
        val notificationPermissionRequested: Boolean = false,
        /** Включён режим «только Wi-Fi»: загрузки стартуют лишь по безлимитной сети. */
        val wifiOnly: Boolean = false,
        /** Сейчас есть безлимитная сеть. */
        val unmeteredNetwork: Boolean = true,
    ) : UiState {
        /** Суммарный объём скачанных данных в байтах по всем загрузкам. */
        val occupiedBytes: Long
            get() = items.sumOf { it.bytesDownloaded.coerceAtLeast(0L) }

        /** Очередь стоит из-за режима «только Wi-Fi», а безлимитной сети нет. */
        val waitingForWifi: Boolean
            get() = wifiOnly && !unmeteredNetwork

        /** Есть загрузки в очереди или в работе, которые можно приостановить разом. */
        val canPauseAll: Boolean
            get() = items.any {
                it.status == VideoDownloadStatus.Queued || it.status == VideoDownloadStatus.Downloading
            }

        /** Есть загрузки на паузе, которые можно возобновить разом. */
        val canResumeAll: Boolean
            get() = items.any { it.status == VideoDownloadStatus.Paused }
    }

    sealed interface Event : UiEvent {
        /** Показан системный запрос разрешения на уведомления: запоминаем это. */
        data object NotificationPermissionRequested : Event

        data object BackSelected : Event
        data class ItemSelected(val id: Long) : Event
        data class DetailsSelected(val animeId: Int) : Event
        data class DeleteSelected(val id: Long) : Event
        data object DeleteConfirmed : Event
        data object DeleteDismissed : Event
        data class PauseSelected(val id: Long) : Event
        data object PauseAllSelected : Event
        data object ResumeAllSelected : Event
        data class ResumeSelected(val id: Long) : Event
        data class RestartSelected(val id: Long) : Event
        data class ExportSelected(val id: Long) : Event
        data object ReExportConfirmed : Event
        data object ReExportDismissed : Event
        data object ExportAllSelected : Event
        data object ExportAllConfirmed : Event
        data object ExportAllDismissed : Event
        data class ExportDirectoryGranted(val uri: String) : Event
        data class CancelExportSelected(val id: Long) : Event
    }

    sealed interface Effect : UiEffect {
        data object OpenExportDirectoryPicker : Effect
        data object ExportDirectorySelectionFailed : Effect
    }
}
