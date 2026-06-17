package su.afk.yummy.tv.feature.collection

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.model.CollectionVote

class CollectionState {
    data class State(
        val isLoading: Boolean = true,
        val isVoteLoading: Boolean = false,
        val collection: CollectionDetail? = null,
        val error: String? = null,
        val focusedItemId: Int? = null,
        val restoreFocusedItemOnEnter: Boolean = false,
        val firstVisibleItemIndex: Int = 0,
        val firstVisibleItemScrollOffset: Int = 0,
    ) : UiState

    /** Пользовательские действия на экране коллекции. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь запросил повторную загрузку коллекции. */
        data object RetrySelected : Event

        /** Пользователь выбрал аниме с указанным идентификатором. */
        data class AnimeSelected(val animeId: Int) : Event

        /** Пользователь выбрал лайк или дизлайк для коллекции. */
        data class VoteSelected(val vote: CollectionVote) : Event

        /** Фокус переместился на аниме с указанным идентификатором. */
        data class ItemFocused(val animeId: Int) : Event

        /** Сетка коллекции прокрутилась к указанной позиции и смещению. */
        data class GridScrolled(val index: Int, val offset: Int) : Event

        /** UI завершил восстановление фокуса на элементе. */
        data object FocusedItemRestoreHandled : Event
    }

    sealed interface Effect : UiEffect
}
