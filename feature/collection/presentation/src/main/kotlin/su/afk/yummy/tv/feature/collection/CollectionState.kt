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
        val currentUserId: Int = 0,
        val collection: CollectionDetail? = null,
        val error: String? = null,
        val firstVisibleItemIndex: Int = 0,
        val firstVisibleItemScrollOffset: Int = 0,
        val isEditDialogVisible: Boolean = false,
        val editTitle: String = "",
        val editDescription: String = "",
        val editIsPublic: Boolean = false,
        val isUpdating: Boolean = false,
        val isDeleteDialogVisible: Boolean = false,
        val isDeleting: Boolean = false,
    ) : UiState {
        val isOwner: Boolean
            get() = currentUserId > 0 && collection?.ownerId == currentUserId
    }

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

        data object EditSelected : Event
        data object EditDismissed : Event
        data object EditConfirmed : Event
        data class EditTitleChanged(val title: String) : Event
        data class EditDescriptionChanged(val description: String) : Event
        data class EditPublicChanged(val isPublic: Boolean) : Event
        data object DeleteSelected : Event
        data object DeleteDismissed : Event
        data object DeleteConfirmed : Event
        data object CommentsSelected : Event

        /** Сетка коллекции прокрутилась к указанной позиции и смещению. */
        data class GridScrolled(val index: Int, val offset: Int) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
    }
}
