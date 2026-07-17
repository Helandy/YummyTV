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
        val isCreateDialogVisible: Boolean = false,
        val createTitle: String = "",
        val createDescription: String = "",
        val isCreatePublic: Boolean = true,
        val isCreating: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetrySelected : Event
        data object CreateSelected : Event
        data object CreateDismissed : Event
        data object CreateConfirmed : Event
        data class CreateTitleChanged(val title: String) : Event
        data class CreateDescriptionChanged(val description: String) : Event
        data class CreatePublicChanged(val isPublic: Boolean) : Event
        data class CollectionSelected(val collectionId: Int) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
    }
}
