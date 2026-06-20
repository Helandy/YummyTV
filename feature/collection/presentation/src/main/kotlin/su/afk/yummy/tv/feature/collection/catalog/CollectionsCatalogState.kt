package su.afk.yummy.tv.feature.collection.catalog

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.collection.model.CollectionSummary

class CollectionsCatalogState {
    data class State(
        val items: Flow<PagingData<CollectionSummary>> = flowOf(PagingData.empty()),
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetrySelected : Event
        data class CollectionSelected(val collectionId: Int) : Event
    }

    sealed interface Effect : UiEffect
}
