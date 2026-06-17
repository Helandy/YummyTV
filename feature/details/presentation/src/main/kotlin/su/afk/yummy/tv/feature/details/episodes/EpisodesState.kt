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
    ) : UiState

    /** Пользовательские действия на экране эпизодов. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь открыл озвучки для указанного эпизода. */
        data class EpisodeDubbingsSelected(val episode: String) : Event

        /** Пользователь выбрал видео для просмотра. */
        data class VideoSelected(val video: AnimeVideo) : Event

        /** Пользователь закрыл выбор балансера. */
        data object BalancerPickerDismissed : Event

        /** Пользователь подтвердил видео для запуска после выбора балансера. */
        data class BalancerConfirmed(val video: AnimeVideo) : Event
    }

    sealed interface Effect : UiEffect
}
