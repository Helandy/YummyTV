package su.afk.yummy.tv.feature.search

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.search.model.SearchFilterOptions
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.model.SearchItem
import su.afk.yummy.tv.domain.search.model.SearchSort

class SearchState {
    data class State(
        val query: String = "",
        val items: List<SearchItem> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val canLoadMore: Boolean = false,
        val offset: Int = 0,
        val focusedItemId: Int? = null,
        val restoreFocusedItemOnEnter: Boolean = false,
        val filters: SearchFilters = SearchFilters.EMPTY,
        val draftFilters: SearchFilters = SearchFilters.EMPTY,
        val filterOptions: SearchFilterOptions = SearchFilterOptions(),
        val isFilterPanelOpen: Boolean = false,
        val isLoadingFilterOptions: Boolean = false,
    ) : UiState

    /** Пользовательские действия на экране поиска. */
    sealed interface Event : UiEvent {
        /** Пользователь изменил поисковый запрос. */
        data class QueryChanged(val query: String) : Event

        /** Внешний источник отправил поисковый запрос. */
        data class ExternalSearchSubmitted(val query: String) : Event

        /** Пользователь выбрал аниме с указанным идентификатором. */
        data class ItemSelected(val animeId: Int) : Event

        /** Фокус переместился на аниме с указанным идентификатором. */
        data class ItemFocused(val animeId: Int) : Event

        /** Пользователь отправил текущий поисковый запрос. */
        data object SearchSubmitted : Event

        /** Пользователь запросил повторную загрузку результатов. */
        data object RetrySelected : Event

        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь запросил загрузку следующей страницы результатов. */
        data object LoadMore : Event

        /** Пользователь открыл панель фильтров. */
        data object OpenFilters : Event

        /** Пользователь закрыл панель фильтров. */
        data object CloseFilters : Event

        /** Пользователь применил выбранные фильтры. */
        data object ApplyFilters : Event

        /** Пользователь сбросил применённые фильтры. */
        data object ResetFilters : Event

        /** Пользователь сбросил черновые изменения фильтров. */
        data object ResetDraftFilters : Event

        /** Пользователь переключил жанр с указанным идентификатором. */
        data class GenreToggled(val id: String) : Event

        /** Пользователь переключил исключённый жанр с указанным идентификатором. */
        data class ExcludedGenreToggled(val id: String) : Event

        /** Пользователь переключил тип аниме с указанным идентификатором. */
        data class TypeToggled(val id: String) : Event

        /** Пользователь переключил статус аниме с указанным идентификатором. */
        data class StatusToggled(val id: String) : Event

        /** Пользователь переключил сезон с указанным идентификатором. */
        data class SeasonToggled(val id: String) : Event

        /** Пользователь переключил возрастной рейтинг с указанным значением. */
        data class AgeRatingToggled(val value: Int) : Event

        /** Пользователь изменил нижнюю границу года выпуска. */
        data class FromYearChanged(val year: Int?) : Event

        /** Пользователь изменил верхнюю границу года выпуска. */
        data class ToYearChanged(val year: Int?) : Event

        /** Пользователь выбрал сортировку результатов. */
        data class SortSelected(val sort: SearchSort) : Event

        /** Пользователь переключил направление сортировки. */
        data object SortDirectionToggled : Event

        /** UI завершил восстановление фокуса на элементе. */
        data object FocusedItemRestoreHandled : Event
    }

    sealed interface Effect : UiEffect
}
