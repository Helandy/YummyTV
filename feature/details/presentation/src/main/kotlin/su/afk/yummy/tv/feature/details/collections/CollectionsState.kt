package su.afk.yummy.tv.feature.details.collections

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary

class CollectionsState {
    data class State(
        val isLoading: Boolean = true,
        val collections: List<AnimeCollectionSummary> = emptyList(),
        val error: String? = null,
    ) : UiState

    /** Пользовательские действия на экране коллекций тайтла. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь запросил повторную загрузку коллекций. */
        data object RetrySelected : Event

        /** Пользователь выбрал коллекцию с указанным идентификатором. */
        data class CollectionSelected(val collectionId: Int) : Event
    }

    sealed interface Effect : UiEffect
}
