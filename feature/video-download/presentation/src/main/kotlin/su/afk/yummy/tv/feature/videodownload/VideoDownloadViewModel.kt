package su.afk.yummy.tv.feature.videodownload

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.videodownload.usecase.CancelOrDeleteVideoDownloadUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.ObserveVideoDownloadsUseCase
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import javax.inject.Inject

@HiltViewModel
class VideoDownloadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val observeVideoDownloads: ObserveVideoDownloadsUseCase,
    private val cancelOrDeleteVideoDownload: CancelOrDeleteVideoDownloadUseCase,
    private val playerNavigator: IPlayerNavigator,
) : BaseViewModelNew<VideoDownloadState.State, VideoDownloadState.Event, VideoDownloadState.Effect>(
    savedStateHandle
) {

    init {
        observeVideoDownloads()
            .onEach { items -> setState { copy(items = items) } }
            .launchIn(viewModelScope)
    }

    override fun createInitialState() = VideoDownloadState.State()

    override fun onEvent(event: VideoDownloadState.Event) {
        when (event) {
            VideoDownloadState.Event.BackSelected -> nav.back()
            is VideoDownloadState.Event.ItemSelected -> {
                nav.navigate(playerNavigator.getDownloadedPlayerDest(event.id))
            }

            is VideoDownloadState.Event.DeleteSelected -> {
                viewModelScope.launch {
                    cancelOrDeleteVideoDownload(event.id)
                }
            }
        }
    }
}
