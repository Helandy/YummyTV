package su.afk.yummy.tv.feature.videodownload

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState

class VideoDownloadState {
    data object State : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
    }

    sealed interface Effect : UiEffect
}
