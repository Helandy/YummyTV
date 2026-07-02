package su.afk.yummy.tv.feature.videodownload

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStreamRefreshResult
import su.afk.yummy.tv.domain.videodownload.usecase.CancelOrDeleteVideoDownloadUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.GetVideoDownloadUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.ObserveVideoDownloadsUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.PauseVideoDownloadUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.RefreshVideoDownloadStreamUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.RestartVideoDownloadUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.UpdateVideoDownloadStatusUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.videodownload.presentation.R
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
    private val getVideoDownload: GetVideoDownloadUseCase,
    private val updateVideoDownloadStatus: UpdateVideoDownloadStatusUseCase,
    private val refreshVideoDownloadStream: RefreshVideoDownloadStreamUseCase,
    private val playerNavigator: IPlayerNavigator,
    private val detailsNavigator: IDetailsNavigator,
    private val strings: StringProvider,
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

            is VideoDownloadState.Event.DetailsSelected -> {
                if (event.animeId > 0) {
                    nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
                }
            }

            is VideoDownloadState.Event.DeleteSelected -> {
                viewModelScope.launch {
                    cancelOrDeleteVideoDownload(event.id)
                }
            }

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
        val item = getVideoDownload(id) ?: return
        updateVideoDownloadStatus(
            id = id,
            status = VideoDownloadStatus.Resolving,
            errorMessage = null,
        )

        val refreshResult = runCatching {
            refreshVideoDownloadStream(
                item = item,
                autoQualityLabel = strings.get(R.string.video_download_quality_auto),
            )
        }
            .getOrElse { throwable ->
                updateVideoDownloadStatus(
                    id = id,
                    status = VideoDownloadStatus.Failed,
                    errorMessage = throwable.localizedMessage
                        ?: strings.get(R.string.video_download_refresh_error),
                )
                return
            }

        when (refreshResult) {
            is VideoDownloadStreamRefreshResult.Success -> {
                restartVideoDownload(id, refreshResult.stream)
            }

            is VideoDownloadStreamRefreshResult.Failure -> {
                updateVideoDownloadStatus(
                    id = id,
                    status = VideoDownloadStatus.Failed,
                    errorMessage = refreshResult.message.ifBlank {
                        strings.get(R.string.video_download_refresh_error)
                    },
                )
            }
        }
    }
}
