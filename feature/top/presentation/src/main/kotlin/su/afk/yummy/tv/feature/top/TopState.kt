package su.afk.yummy.tv.feature.top

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.top.model.AnimeTopItem
import su.afk.yummy.tv.domain.top.model.AnimeTopType

class TopState {
    data class State(
        val selectedType: AnimeTopType = AnimeTopType.TV,
        val items: Flow<PagingData<AnimeTopItem>> = flowOf(PagingData.empty()),
        val showTitleYear: Boolean = false,
    ) : UiState

    /** Пользовательские действия на экране топа аниме. */
    sealed interface Event : UiEvent {
        /** Пользователь выбрал тип рейтинга топа. */
        data class TypeSelected(val type: AnimeTopType) : Event

        /** Пользователь выбрал аниме с указанным идентификатором. */
        data class AnimeSelected(val animeId: Int) : Event

        /** Пользователь запросил повторную загрузку топа. */
        data object RetrySelected : Event
    }

    sealed interface Effect : UiEffect
}
