package su.afk.yummy.tv.feature.details.episodes

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.details.BalancerPickerState
import su.afk.yummy.tv.feature.details.details.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.details.VideosUiState
import su.afk.yummy.tv.feature.details.episodes.dubbings.EpisodeDubbingsState

class EpisodesState {
    data class State(
        val videosState: VideosUiState = VideosUiState.Loading,
        val watchProgress: DetailsWatchProgressIndex = DetailsWatchProgressIndex.Empty,
        val pendingBalancerSelection: BalancerPickerState? = null,
        val pendingEpisodeDubbingSelection: EpisodeDubbingSelection? = null,
        val downloadStatuses: Map<String, EpisodeDownloadUiState> = emptyMap(),
        val resolvingDownloadKeys: Set<String> = emptySet(),
        val pendingDownloadDubbingSelection: EpisodeDownloadDubbingSelection? = null,
        val pendingDownloadBalancerSelection: EpisodeDownloadBalancerSelection? = null,
        val pendingDownloadQualitySelection: EpisodeDownloadQualitySelection? = null,
        val pendingDownloadedEpisodeAction: DownloadedEpisodeAction? = null,
    ) : UiState

    data class EpisodeDubbingSelection(
        val episode: String,
        val options: List<EpisodeDubbingOption>,
    )

    data class EpisodeDubbingOption(
        val video: AnimeVideo,
        val item: EpisodeDubbingsState.DubbingItem,
    )

    data class EpisodeDownloadDubbingSelection(
        val episode: String,
        val options: List<EpisodeDownloadDubbingOption>,
        val hasAlternativeDubbings: Boolean = false,
    )

    data class DownloadedEpisodeAction(
        val downloadId: Long,
        val episode: String,
        val downloadedDubbing: String,
        val playerName: String,
        val qualityLabel: String,
        val bytesDownloaded: Long,
        val videos: List<AnimeVideo>,
        val hasAlternativeDubbings: Boolean,
    )

    data class EpisodeDownloadDubbingOption(
        val videos: List<AnimeVideo>,
        val title: String,
        val subtitle: String?,
        val status: EpisodeDownloadUiState?,
        val resolving: Boolean,
    )

    data class EpisodeDownloadBalancerSelection(
        val episode: String,
        val dubbing: String,
        val options: List<EpisodeDownloadBalancerOption>,
    )

    data class EpisodeDownloadBalancerOption(
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
        Paused,
        Downloaded,
        Failed,
    }

    data class EpisodeDownloadUiState(
        val downloadId: Long,
        val dubbing: String,
        val playerName: String,
        val qualityLabel: String,
        val bytesDownloaded: Long,
        val status: EpisodeDownloadUiStatus,
        val progress: Float,
        val errorMessage: String?,
    )

    /** Пользовательские действия на экране эпизодов. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь открыл озвучки для указанного эпизода. */
        data class EpisodeDubbingsSelected(val episode: String) : Event

        /** Пользователь выбрал серию на ТВ. */
        data class TvEpisodeSelected(val video: AnimeVideo) : Event

        /** Пользователь подтвердил балансер на ТВ. */
        data class TvBalancerConfirmed(val video: AnimeVideo) : Event

        /** Пользователь выбрал озвучку в ТВ-диалоге. */
        data class EpisodeDubbingSelected(val video: AnimeVideo) : Event

        /** Пользователь закрыл ТВ-диалог выбора озвучки. */
        data object EpisodeDubbingPickerDismissed : Event

        /** Пользователь выбрал видео для просмотра. */
        data class VideoSelected(val video: AnimeVideo) : Event

        /** Пользователь нажал скачивание серии. */
        data class EpisodeDownloadSelected(val videos: List<AnimeVideo>) : Event

        /** Пользователь нажал галочку у скачанной серии. */
        data class DownloadedEpisodeSelected(
            val videos: List<AnimeVideo>,
            val download: EpisodeDownloadUiState,
        ) : Event

        /** Пользователь выбрал воспроизведение скачанной серии. */
        data object PlayDownloadedEpisodeSelected : Event

        /** Пользователь выбрал перекачивание серии с другой озвучкой. */
        data object RedownloadDubbingSelected : Event

        /** Пользователь удалил скачанную серию из панели действий. */
        data object DeleteDownloadedEpisodeSelected : Event

        /** Пользователь закрыл действия со скачанной серией. */
        data object DownloadedEpisodeActionDismissed : Event

        /** Пользователь выбрал озвучку для скачивания. */
        data class DownloadDubbingSelected(val videos: List<AnimeVideo>) : Event

        /** Пользователь закрыл выбор озвучки для скачивания. */
        data object DownloadDubbingPickerDismissed : Event

        /** Пользователь выбрал балансер для скачивания. */
        data class DownloadBalancerSelected(val video: AnimeVideo) : Event

        /** Пользователь закрыл выбор балансера для скачивания. */
        data object DownloadBalancerPickerDismissed : Event

        /** Пользователь выбрал качество скачивания. */
        data class DownloadQualitySelected(val option: EpisodeDownloadQualityOption) : Event

        /** Пользователь закрыл выбор качества скачивания. */
        data object DownloadQualityPickerDismissed : Event

        /** Пользователь закрыл выбор балансера. */
        data object BalancerPickerDismissed : Event

        /** Пользователь нажал иконку хранилища у скачиваемой серии, чтобы перейти в загрузки. */
        data object OpenDownloadsScreenSelected : Event

        /** Пользователь подтвердил видео для запуска после выбора балансера. */
        data class BalancerConfirmed(val video: AnimeVideo) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
    }
}
