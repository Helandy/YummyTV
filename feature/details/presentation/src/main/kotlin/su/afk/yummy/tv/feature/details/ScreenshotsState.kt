package su.afk.yummy.tv.feature.details

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.anime.AnimeScreenshot

class ScreenshotsState {
    data class State(
        val isLoading: Boolean = true,
        val title: String = "",
        val screenshots: List<AnimeScreenshot> = emptyList(),
        val selectedIndex: Int? = null,
        val error: String? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data class ScreenshotSelected(val index: Int) : Event
        data object ScreenshotDismissed : Event
        data object PreviousSelected : Event
        data object NextSelected : Event
    }

    sealed interface Effect : UiEffect
}
