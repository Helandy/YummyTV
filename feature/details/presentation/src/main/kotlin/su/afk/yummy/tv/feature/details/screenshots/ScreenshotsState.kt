package su.afk.yummy.tv.feature.details.screenshots

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.anime.model.AnimeScreenshot

class ScreenshotsState {
    data class State(
        val isLoading: Boolean = true,
        val title: String = "",
        val screenshots: List<AnimeScreenshot> = emptyList(),
        val selectedIndex: Int? = null,
        val error: String? = null,
    ) : UiState

    /** Пользовательские действия на экране скриншотов. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь открыл скриншот с указанным индексом. */
        data class ScreenshotSelected(val index: Int) : Event

        /** Пользователь закрыл выбранный скриншот. */
        data object ScreenshotDismissed : Event

        /** Пользователь перешёл к предыдущему скриншоту. */
        data object PreviousSelected : Event

        /** Пользователь перешёл к следующему скриншоту. */
        data object NextSelected : Event

        /** Пользователь запросил повторную загрузку скриншотов. */
        data object RetrySelected : Event
    }

    sealed interface Effect : UiEffect
}
