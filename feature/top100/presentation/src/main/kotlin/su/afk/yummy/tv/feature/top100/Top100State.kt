package su.afk.yummy.tv.feature.top100

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.anime.model.AnimePreview
import su.afk.yummy.tv.domain.top100.model.AnimeTopItem
import su.afk.yummy.tv.domain.top100.model.AnimeTopType

class Top100State {
    data class State(
        val selectedType: AnimeTopType = AnimeTopType.TV,
        val items: List<AnimeTopItem> = emptyList(),
        val isLoading: Boolean = true,
        val isLoadingMore: Boolean = false,
        val error: String? = null,
        val canLoadMore: Boolean = true,
        val offset: Int = 0,
        val focusedItemId: Int? = null,
        val focusedPreview: AnimePreview? = null,
        val restoreFocusedItemOnEnter: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class TypeSelected(val type: AnimeTopType) : Event
        data class AnimeSelected(val animeId: Int) : Event
        data class ItemFocused(val animeId: Int) : Event
        data object FocusedItemRestoreHandled : Event
        data object LoadMore : Event
        data object RetrySelected : Event
    }

    sealed interface Effect : UiEffect
}
