package su.afk.yummy.tv.feature.details.episodes.dubbings

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState

class EpisodeDubbingsState {
    data class State(
        val episode: String = "",
        val isLoading: Boolean = true,
        val error: String? = null,
        val dubbings: List<DubbingItem> = emptyList(),
    ) : UiState

    data class DubbingItem(
        val name: String,
        val views: Int,
        val episodeCount: Int,
        val supportedBalancers: String,
    )

    /** Пользовательские действия на экране озвучек эпизода. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь выбрал озвучку для запуска текущей серии. */
        data class DubbingSelected(val name: String) : Event
    }

    sealed interface Effect : UiEffect
}
