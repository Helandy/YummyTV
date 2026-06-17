package su.afk.yummy.tv.feature.collection.catalog

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.collection.model.CollectionSummary

class CollectionsCatalogState {
    data class State(
        val items: List<CollectionSummary> = emptyList(),
        val isLoading: Boolean = true,
        val isLoadingMore: Boolean = false,
        val error: String? = null,
        val offset: Int = 0,
        val canLoadMore: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetrySelected : Event
        data object LoadMoreSelected : Event
        data class CollectionSelected(val collectionId: Int) : Event
    }

    sealed interface Effect : UiEffect
}
