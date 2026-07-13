package su.afk.yummy.tv.feature.videodownload

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem

class VideoDownloadState {
    data class State(
        val items: List<VideoDownloadItem> = emptyList(),
        val pendingDeleteItem: VideoDownloadItem? = null,
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
    }

    sealed interface Effect : UiEffect
}
