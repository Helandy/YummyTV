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
import su.afk.yummy.tv.domain.videodownload.usecase.PauseVideoDownloadUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.RestartVideoDownloadUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
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
    private val pauseVideoDownload: PauseVideoDownloadUseCase,
    private val restartVideoDownload: RestartVideoDownloadUseCase,
    private val playerNavigator: IPlayerNavigator,
    private val detailsNavigator: IDetailsNavigator,
) : BaseViewModelNew<VideoDownloadState.State, VideoDownloadState.Event, VideoDownloadState.Effect>(
    savedStateHandle
) {

    init {
        observeVideoDownloads()
            .onEach { items ->
                setState { copy(items = items) }
            }
            .launchIn(viewModelScope)
    }

    override fun createInitialState() = VideoDownloadState.State()

    override fun onEvent(event: VideoDownloadState.Event) {
        when (event) {
            VideoDownloadState.Event.BackSelected -> nav.back()
            is VideoDownloadState.Event.ItemSelected -> {
                nav.navigate(playerNavigator.getDownloadedPlayerDest(event.id))
            }

            is VideoDownloadState.Event.DetailsSelected -> {
                if (event.animeId > 0) {
                    nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
                }
            }

            is VideoDownloadState.Event.DeleteSelected -> {
                setState {
                    copy(pendingDeleteItem = items.firstOrNull { it.id == event.id })
                }
            }

            VideoDownloadState.Event.DeleteConfirmed -> {
                val downloadId = currentState.pendingDeleteItem?.id ?: return
                setState { copy(pendingDeleteItem = null) }
                viewModelScope.launch {
                    cancelOrDeleteVideoDownload(downloadId)
                }
            }

            VideoDownloadState.Event.DeleteDismissed ->
                setState { copy(pendingDeleteItem = null) }

            is VideoDownloadState.Event.PauseSelected -> {
                viewModelScope.launch {
                    pauseVideoDownload(event.id)
                }
            }

            is VideoDownloadState.Event.ResumeSelected -> {
                viewModelScope.launch {
                    restartDownload(event.id)
                }
            }

            is VideoDownloadState.Event.RestartSelected -> {
                viewModelScope.launch {
                    restartDownload(event.id)
                }
            }
        }
    }

    private suspend fun restartDownload(id: Long) {
        restartVideoDownload(id)
    }
}
