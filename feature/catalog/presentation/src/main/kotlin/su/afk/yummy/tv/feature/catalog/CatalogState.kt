package su.afk.yummy.tv.feature.catalog

import androidx.paging.PagingData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import su.afk.yummy.tv.core.model.settings.TitleCardLayout
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.catalog.model.SearchFilterOptions
import su.afk.yummy.tv.domain.catalog.model.SearchFilters
import su.afk.yummy.tv.domain.catalog.model.SearchItem
import su.afk.yummy.tv.domain.catalog.model.SearchSort

class CatalogState {
    data class State(
        val query: String = "",
        val results: Flow<PagingData<SearchItem>> = flowOf(PagingData.empty()),
        val isSearchActive: Boolean = false,
        /** Запрос введён, но поиск ещё ждёт паузы в наборе (debounce). */
        val isSearchPending: Boolean = false,
        val error: UiText? = null,
        val filters: SearchFilters = SearchFilters.EMPTY,
        val draftFilters: SearchFilters = SearchFilters.EMPTY,
        val filterOptions: SearchFilterOptions = SearchFilterOptions(),
        val isFilterPanelOpen: Boolean = false,
        val isLoadingFilterOptions: Boolean = false,
        val isRandomAnimeLoading: Boolean = false,
        val showTitleYear: Boolean = false,
        val titleCardLayout: TitleCardLayout = TitleCardLayout.TILE,
        /**
         * Другая фича попросила фокус в поле поиска. Это состояние, а не эффект: запрос приходит
         * до того, как экран начнёт собирать эффекты, и снимается событием [Event.SearchFocusHandled].
         */
        val isSearchFocusRequested: Boolean = false,
        /** Последние поисковые запросы, от новых к старым. */
        val searchHistory: ImmutableList<String> = persistentListOf(),
    ) : UiState

    /** Пользовательские действия на экране поиска. */
    sealed interface Event : UiEvent {
        /** Пользователь изменил поисковый запрос. */
        data class QueryChanged(val query: String) : Event

        /** Внешний источник отправил поисковый запрос. */
        data class ExternalSearchSubmitted(val query: String) : Event

        /** Пользователь выбрал аниме с указанным идентификатором. */
        data class ItemSelected(val animeId: Int) : Event

        /** Пользователь отправил текущий поисковый запрос. */
        data object SearchSubmitted : Event

        /** Пользователь запросил повторную загрузку результатов. */
        data object RetrySelected : Event

        /** Пользователь запросил случайное аниме из всего каталога. */
        data object RandomAnimeSelected : Event

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

        /** Пользователь выбрал тип аниме с указанным идентификатором (повторный выбор снимает его). */
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

        /** Экран поставил фокус в поле поиска по запросу [State.isSearchFocusRequested]. */
        data object SearchFocusHandled : Event

        /** Пользователь выбрал запрос из истории поиска. */
        data class SearchHistorySelected(val query: String) : Event

        /** Пользователь очистил историю поиска. */
        data object SearchHistoryCleared : Event
    }

    sealed interface Effect : UiEffect
}
