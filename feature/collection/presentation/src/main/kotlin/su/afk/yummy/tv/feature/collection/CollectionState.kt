package su.afk.yummy.tv.feature.collection

import su.afk.yummy.tv.core.model.settings.TitleCardLayout
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.model.CollectionVote

class CollectionState {
    data class State(
        val isLoading: Boolean = true,
        val isVoteLoading: Boolean = false,
        val currentUserId: Int = 0,
        val collection: CollectionDetail? = null,
        val error: UiText? = null,
        val firstVisibleItemIndex: Int = 0,
        val firstVisibleItemScrollOffset: Int = 0,
        val isEditDialogVisible: Boolean = false,
        val editTitle: String = "",
        val editDescription: String = "",
        val editIsPublic: Boolean = false,
        val isUpdating: Boolean = false,
        val isDeleteDialogVisible: Boolean = false,
        val isDeleting: Boolean = false,
        val titleCardLayout: TitleCardLayout = TitleCardLayout.TILE,
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

        /** Пользователь нажал на автора коллекции. */
        data object AuthorSelected : Event

        /** Сетка коллекции прокрутилась к указанной позиции и смещению. */
        data class GridScrolled(val index: Int, val offset: Int) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
    }
}
