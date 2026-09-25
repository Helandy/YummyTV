package su.afk.yummy.tv.feature.home

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.feature.home.model.HomeAnnouncement

class HomeState {
    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val feed: HomeFeed? = null,
        /** Есть ли секция расписания в исходной ленте (до фильтрации из [feed]). */
        val hasSchedule: Boolean = false,
        val error: String? = null,
        val continueWatching: ImmutableList<HomeContinueWatchingItem> = persistentListOf(),
        val isContinueWatchingLoaded: Boolean = false,
        val supportPromptVisible: Boolean = false,
        val announcement: HomeAnnouncement? = null,
        val bloggerVideos: ImmutableList<BloggerVideo> = persistentListOf(),
        val isBloggerVideosLoading: Boolean = true,
        val bloggerVideosError: String? = null,
        /** Тайтлы, скрытые из блока рекомендаций в текущей сессии. */
        val hiddenRecommendationIds: PersistentSet<Int> = persistentSetOf(),
        /** Тайтлы, для которых сейчас выполняется запрос на скрытие или возврат. */
        val pendingRecommendationIds: PersistentSet<Int> = persistentSetOf(),
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

        /** Пользователь открыл поиск с плашки на главном экране. */
        data object SearchSelected : Event

        data object BloggerVideosSelected : Event

        data object BloggerVideosRetrySelected : Event

        data class BloggerVideoSelected(val video: BloggerVideo) : Event

        /** Пользователь отказался от предложения поддержать проект. */
        data object SupportPromptDismissed : Event

        /** Пользователь закрыл объявление кнопкой ОК. */
        data object AnnouncementDismissed : Event

        /** Пользователь попросил больше не рекомендовать тайтл. */
        data class RecommendationHideRequested(val animeId: Int) : Event

        /** Пользователь вернул скрытый тайтл в рекомендации. */
        data class RecommendationRestoreRequested(val animeId: Int) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
        data class OpenUri(val uri: String) : Effect

        /** Тайтл скрыт из рекомендаций, действие можно откатить. */
        data class ShowRecommendationUndo(val message: String, val animeId: Int) : Effect
    }
}

/** Видео пока не имеет собственного экрана — событие не отправляется. */
fun HomeFeedItemAction.toHomeEventOrNull(): HomeState.Event? = when (this) {
    is HomeFeedItemAction.OpenSeries -> HomeState.Event.AnimeSelected(seriesId)
    is HomeFeedItemAction.OpenCollection -> HomeState.Event.CollectionSelected(collectionId)
    is HomeFeedItemAction.OpenVideo -> null
}
