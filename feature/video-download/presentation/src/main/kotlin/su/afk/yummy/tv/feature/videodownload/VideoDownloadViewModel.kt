package su.afk.yummy.tv.feature.videodownload

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import javax.inject.Inject

@HiltViewModel
class VideoDownloadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
) : BaseViewModelNew<VideoDownloadState.State, VideoDownloadState.Event, VideoDownloadState.Effect>(
    savedStateHandle
) {

    override fun createInitialState() = VideoDownloadState.State

    override fun onEvent(event: VideoDownloadState.Event) {
        when (event) {
            VideoDownloadState.Event.BackSelected -> nav.back()
        }
    }
}
