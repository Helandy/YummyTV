package su.afk.yummy.tv.feature.videodownload

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoExportDestination

class VideoDownloadState {
    data class State(
        val items: List<VideoDownloadItem> = emptyList(),
        val pendingDeleteItem: VideoDownloadItem? = null,
        val exportDestination: VideoExportDestination? = null,
        val pendingBulkExportCount: Int = 0,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data class ItemSelected(val id: Long) : Event
        data class DetailsSelected(val animeId: Int) : Event
        data class DeleteSelected(val id: Long) : Event
        data object DeleteConfirmed : Event
        data object DeleteDismissed : Event
        data class PauseSelected(val id: Long) : Event
        data class ResumeSelected(val id: Long) : Event
        data class RestartSelected(val id: Long) : Event
        data class ExportSelected(val id: Long) : Event
        data object ExportAllSelected : Event
        data object ExportAllConfirmed : Event
        data object ExportAllDismissed : Event
        data object ExportDirectorySelected : Event
        data class ExportDirectoryGranted(val uri: String) : Event
        data class CancelExportSelected(val id: Long) : Event
    }

    sealed interface Effect : UiEffect {
        data object OpenExportDirectoryPicker : Effect
        data object ExportDirectorySelectionFailed : Effect
    }
}
