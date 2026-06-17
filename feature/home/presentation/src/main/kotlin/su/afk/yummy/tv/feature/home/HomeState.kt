package su.afk.yummy.tv.feature.home

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed

class HomeState {
    data class State(
        val isLoading: Boolean = true,
        val feed: HomeFeed? = null,
        val error: String? = null,
        val focusedItemId: Int? = null,
        val restoreFocusedItemOnEnter: Boolean = false,
        val focusedSectionId: String? = null,
        val continueWatching: List<HomeContinueWatchingItem> = emptyList(),
        val isContinueWatchingLoaded: Boolean = false,
        val continueWatchingRestoreToken: Int = 0,
        val continueWatchingRestoreKey: String? = null,
        val supportPromptVisible: Boolean = false,
    ) : UiState

    /** Пользовательские действия на главном экране. */
    sealed interface Event : UiEvent {
        /** Пользователь выбрал аниме из указанной секции главной ленты. */
        data class AnimeSelected(
            val seriesId: Int,
            val sourceSectionId: String? = null,
            val displayId: Int? = null,
        ) : Event

        /** Пользователь выбрал коллекцию из указанной секции главной ленты. */
        data class CollectionSelected(
            val collectionId: Int,
            val sourceSectionId: String? = null,
            val displayId: Int? = null,
        ) : Event

        /** Фокус переместился на элемент главной ленты. */
        data class ItemFocused(val sectionId: String, val displayId: Int, val animeId: Int?) : Event

        /** Пользователь выбрал элемент продолжения просмотра. */
        data class ContinueWatchingSelected(val entry: HomeContinueWatchingItem) : Event

        /** Фокус переместился на элемент продолжения просмотра. */
        data class ContinueWatchingFocused(val entry: HomeContinueWatchingItem) : Event

        /** UI завершил восстановление фокуса на элементе. */
        data object FocusedItemRestoreHandled : Event

        /** Экран снова стал активным для пользователя. */
        data object ScreenResumed : Event

        /** Пользователь запросил обновление главной ленты. */
        data object RefreshRequested : Event

        /** Пользователь запросил повторную загрузку главной ленты. */
        data object RetrySelected : Event

        /** Пользователь открыл каталог коллекций с главного экрана. */
        data object CollectionsCatalogSelected : Event

        /** Пользователь отказался от предложения поддержать проект. */
        data object SupportPromptDismissed : Event
    }

    sealed interface Effect : UiEffect
}
