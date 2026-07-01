package su.afk.yummy.tv.feature.details.episodes

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.details.BalancerPickerState
import su.afk.yummy.tv.feature.details.details.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.details.VideosUiState

class EpisodesState {
    data class State(
        val videosState: VideosUiState = VideosUiState.Loading,
        val watchProgress: DetailsWatchProgressIndex = DetailsWatchProgressIndex.Empty,
        val pendingBalancerSelection: BalancerPickerState? = null,
        val downloadStatuses: Map<String, EpisodeDownloadUiState> = emptyMap(),
        val resolvingDownloadKeys: Set<String> = emptySet(),
        val pendingDownloadDubbingSelection: EpisodeDownloadDubbingSelection? = null,
        val pendingDownloadQualitySelection: EpisodeDownloadQualitySelection? = null,
    ) : UiState

    data class EpisodeDownloadDubbingSelection(
        val episode: String,
        val options: List<EpisodeDownloadDubbingOption>,
    )

    data class EpisodeDownloadDubbingOption(
        val video: AnimeVideo,
        val title: String,
        val subtitle: String?,
        val status: EpisodeDownloadUiState?,
        val resolving: Boolean,
    )

    data class EpisodeDownloadQualitySelection(
        val videoId: Int,
        val episode: String,
        val options: List<EpisodeDownloadQualityOption>,
    )

    data class EpisodeDownloadQualityOption(
        val label: String,
        val url: String,
    )

    enum class EpisodeDownloadUiStatus {
        Queued,
        Downloading,
        Downloaded,
        Failed,
    }

    data class EpisodeDownloadUiState(
        val status: EpisodeDownloadUiStatus,
        val progress: Float,
    )

    /** Пользовательские действия на экране эпизодов. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь открыл озвучки для указанного эпизода. */
        data class EpisodeDubbingsSelected(val episode: String) : Event

        /** Пользователь выбрал видео для просмотра. */
        data class VideoSelected(val video: AnimeVideo) : Event

        /** Пользователь нажал скачивание серии. */
        data class EpisodeDownloadSelected(val videos: List<AnimeVideo>) : Event

        /** Пользователь выбрал озвучку для скачивания. */
        data class DownloadDubbingSelected(val video: AnimeVideo) : Event

        /** Пользователь закрыл выбор озвучки для скачивания. */
        data object DownloadDubbingPickerDismissed : Event

        /** Пользователь выбрал качество скачивания. */
        data class DownloadQualitySelected(val option: EpisodeDownloadQualityOption) : Event

        /** Пользователь закрыл выбор качества скачивания. */
        data object DownloadQualityPickerDismissed : Event

        /** Пользователь закрыл выбор балансера. */
        data object BalancerPickerDismissed : Event

        /** Пользователь подтвердил видео для запуска после выбора балансера. */
        data class BalancerConfirmed(val video: AnimeVideo) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
    }
}
