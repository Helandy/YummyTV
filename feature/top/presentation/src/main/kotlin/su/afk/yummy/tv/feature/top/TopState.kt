package su.afk.yummy.tv.feature.top

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.top.model.AnimeTopItem
import su.afk.yummy.tv.domain.top.model.AnimeTopType

class TopState {
    data class State(
        val selectedType: AnimeTopType = AnimeTopType.TV,
        val items: List<AnimeTopItem> = emptyList(),
        val isLoading: Boolean = true,
        val isLoadingMore: Boolean = false,
        val error: String? = null,
        val canLoadMore: Boolean = true,
        val offset: Int = 0,
        val focusedItemId: Int? = null,
        val restoreFocusedItemOnEnter: Boolean = false,
    ) : UiState

    /** Пользовательские действия на экране топа аниме. */
    sealed interface Event : UiEvent {
        /** Пользователь выбрал тип рейтинга топа. */
        data class TypeSelected(val type: AnimeTopType) : Event

        /** Пользователь выбрал аниме с указанным идентификатором. */
        data class AnimeSelected(val animeId: Int) : Event

        /** Фокус переместился на аниме с указанным идентификатором. */
        data class ItemFocused(val animeId: Int) : Event

        /** UI завершил восстановление фокуса на элементе. */
        data object FocusedItemRestoreHandled : Event

        /** Пользователь запросил загрузку следующей страницы топа. */
        data object LoadMore : Event

        /** Пользователь запросил повторную загрузку топа. */
        data object RetrySelected : Event
    }

    sealed interface Effect : UiEffect
}
