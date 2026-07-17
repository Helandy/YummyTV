package su.afk.yummy.tv.feature.home

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed

class HomeState {
    data class State(
        val isLoading: Boolean = true,
        val feed: HomeFeed? = null,
        val error: String? = null,
        val continueWatching: List<HomeContinueWatchingItem> = emptyList(),
        val isContinueWatchingLoaded: Boolean = false,
        val supportPromptVisible: Boolean = false,
        val bloggerVideos: List<BloggerVideo> = emptyList(),
        val isBloggerVideosLoading: Boolean = true,
        val bloggerVideosError: String? = null,
    ) : UiState

    /** Пользовательские действия на главном экране. */
    sealed interface Event : UiEvent {
        /** Пользователь выбрал аниме из указанной секции главной ленты. */
        data class AnimeSelected(val seriesId: Int) : Event

        /** Пользователь выбрал коллекцию из указанной секции главной ленты. */
        data class CollectionSelected(val collectionId: Int) : Event

        /** Пользователь выбрал элемент продолжения просмотра. */
        data class ContinueWatchingSelected(val entry: HomeContinueWatchingItem) : Event

        /** Экран снова стал активным для пользователя. */
        data object ScreenResumed : Event

        /** Пользователь запросил обновление главной ленты. */
        data object RefreshRequested : Event

        /** Пользователь запросил повторную загрузку главной ленты. */
        data object RetrySelected : Event

        /** Пользователь открыл каталог коллекций с главного экрана. */
        data object CollectionsCatalogSelected : Event

        /** Пользователь открыл расписание с главного экрана. */
        data object ScheduleSelected : Event

        /** Пользователь открыл общую ленту рецензий с главного экрана. */
        data object ReviewsSelected : Event

        data object BloggerVideosSelected : Event

        data object BloggerVideosRetrySelected : Event

        data class BloggerVideoSelected(val video: BloggerVideo) : Event

        /** Пользователь отказался от предложения поддержать проект. */
        data object SupportPromptDismissed : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data class OpenUri(val uri: String) : Effect
    }
}
