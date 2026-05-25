package su.afk.yummy.tv.feature.details

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.anime.model.AnimeVideo

class EpisodesState {
    data class State(
        val videosState: VideosUiState = VideosUiState.Loading,
        val watchProgress: Map<String, WatchProgressEntry> = emptyMap(),
        val pendingBalancerSelection: BalancerPickerState? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data class VideoSelected(val video: AnimeVideo) : Event
        data object BalancerPickerDismissed : Event
        data class BalancerConfirmed(val video: AnimeVideo) : Event
    }

    sealed interface Effect : UiEffect
}
