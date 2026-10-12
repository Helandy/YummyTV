package su.afk.yummy.tv.feature.details.collections

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary

class CollectionsState {
    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val collections: ImmutableList<AnimeCollectionSummary> = persistentListOf(),
        val error: UiText? = null,
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
