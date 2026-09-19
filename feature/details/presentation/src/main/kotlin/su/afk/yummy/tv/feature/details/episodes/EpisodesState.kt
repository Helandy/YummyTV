package su.afk.yummy.tv.feature.details.episodes

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import su.afk.yummy.tv.core.model.anime.AnimeEpisodeInfo
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.feature.details.details.model.BalancerPickerState
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
import su.afk.yummy.tv.feature.details.episodes.dubbings.EpisodeDubbingsState
import su.afk.yummy.tv.feature.details.model.DetailsWatchProgressIndex

class EpisodesState {
    @Immutable
    data class State(
        val videosState: VideosUiState = VideosUiState.Loading,
        val watchProgress: DetailsWatchProgressIndex = DetailsWatchProgressIndex.Empty,
        val pendingBalancerSelection: BalancerPickerState? = null,
        val pendingEpisodeDubbingSelection: EpisodeDubbingSelection? = null,
        val downloadStatuses: ImmutableMap<String, EpisodeDownloadUiState> = persistentMapOf(),
        val resolvingDownloadKeys: PersistentSet<String> = persistentSetOf(),
        val pendingDownloadDubbingSelection: EpisodeDownloadDubbingSelection? = null,
        val pendingDownloadBalancerSelection: EpisodeDownloadBalancerSelection? = null,
        val pendingDownloadQualitySelection: EpisodeDownloadQualitySelection? = null,
        val pendingDownloadedEpisodeAction: DownloadedEpisodeAction? = null,
        /** Открытое меню действий над серией (долгое нажатие по карточке). */
        val pendingEpisodeAction: EpisodeAction? = null,
        /** Отложенные серии этого тайтла — нормализованные номера (episodeGroupKey). */
        val watchLaterEpisodes: PersistentSet<String> = persistentSetOf(),
        /** Названия и описания серий из YummyTV API по номеру серии. */
        val episodeInfo: ImmutableMap<String, AnimeEpisodeInfo> = persistentMapOf(),
        /** Серии с раскрытым описанием (мобильная карточка). */
        val expandedEpisodeDescriptions: PersistentSet<String> = persistentSetOf(),
        /** Видео, сгруппированные по серии и отсортированные по номеру. */
        val episodeGroups: ImmutableList<EpisodeGroup> = persistentListOf(),
        /** Озвучка с наибольшим числом просмотров среди kodik-источников. */
        val bestDubbing: String = "",
        /** Приоритетный статус загрузки на серию: busy > paused > downloaded > failed. */
        val resolvedDownloadStatuses: ImmutableMap<String, EpisodeDownloadUiState?> = persistentMapOf(),
    ) : UiState

    @Immutable
    data class EpisodeGroup(
        val episode: String,
        val videos: ImmutableList<AnimeVideo>,
    )

    @Immutable
    data class EpisodeAction(
        val episode: String,
        val videos: ImmutableList<AnimeVideo>,
        /** Текущий статус серии — от него зависит направление действия. */
        val isWatched: Boolean,
        /** Серия уже отложена — от этого зависит направление действия. */
        val isInWatchLater: Boolean,
    )

    @Immutable
    data class EpisodeDubbingSelection(
        val episode: String,
        val options: ImmutableList<EpisodeDubbingOption>,
        val episodeTitle: String? = null,
    )

    data class EpisodeDubbingOption(
        val video: AnimeVideo,
        val item: EpisodeDubbingsState.DubbingItem,
    )

    @Immutable
    data class EpisodeDownloadDubbingSelection(
        val episode: String,
        val options: ImmutableList<EpisodeDownloadDubbingOption>,
        val hasAlternativeDubbings: Boolean = false,
    )

    @Immutable
    data class DownloadedEpisodeAction(
        val downloadId: Long,
        val episode: String,
        val downloadedDubbing: String,
        val playerName: String,
        val qualityLabel: String,
        val bytesDownloaded: Long,
        val videos: ImmutableList<AnimeVideo>,
        val hasAlternativeDubbings: Boolean,
    )

    @Immutable
    data class EpisodeDownloadDubbingOption(
        val videos: ImmutableList<AnimeVideo>,
        val title: String,
        val subtitle: String?,
        val status: EpisodeDownloadUiState?,
        val resolving: Boolean,
        /** Просмотры озвучки по всему тайтлу — как в пикере запуска. */
        val views: Int = 0,
        /** Число серий озвучки по всему тайтлу. */
        val episodeCount: Int = 0,
    )

    @Immutable
    data class EpisodeDownloadBalancerSelection(
        val episode: String,
        val dubbing: String,
        val options: ImmutableList<EpisodeDownloadBalancerOption>,
    )

    data class EpisodeDownloadBalancerOption(
        val video: AnimeVideo,
        val title: String,
        val subtitle: String?,
        val status: EpisodeDownloadUiState?,
        val resolving: Boolean,
    )

    @Immutable
    data class EpisodeDownloadQualitySelection(
        val videoId: Int,
        val episode: String,
        val options: ImmutableList<EpisodeDownloadQualityOption>,
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

        /** Пользователь запросил повторную загрузку списка серий. */
        data object RetryVideosSelected : Event

        /** Пользователь свернул или раскрыл описание серии. */
        data class EpisodeDescriptionToggled(val episode: String) : Event

        /** Пользователь выбрал серию из списка. */
        data class EpisodeSelected(val video: AnimeVideo) : Event

        /** Пользователь выбрал озвучку в диалоге выбора озвучки. */
        data class EpisodeDubbingSelected(val video: AnimeVideo) : Event

        /** Пользователь закрыл диалог выбора озвучки. */
        data object EpisodeDubbingPickerDismissed : Event

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

        /** Пользователь вызвал меню действий над серией долгим нажатием. */
        data class EpisodeActionsRequested(val videos: List<AnimeVideo>) : Event

        /** Пользователь переключил отметку "просмотрено" у серии. */
        data object EpisodeWatchedToggled : Event

        /** Пользователь отложил серию или снял пометку. */
        data object EpisodeWatchLaterToggled : Event

        /** Пользователь закрыл меню действий над серией. */
        data object EpisodeActionsDismissed : Event

        /** Пользователь подтвердил видео для запуска после выбора балансера. */
        data class BalancerConfirmed(val video: AnimeVideo) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
    }
}
