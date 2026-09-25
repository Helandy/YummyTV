package su.afk.yummy.tv.feature.details.details

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.settings.DetailsButtonAction
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.details.model.BalancerPickerState
import su.afk.yummy.tv.feature.details.details.model.DubbingPickerState
import su.afk.yummy.tv.feature.details.details.model.SubscriptionOption
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
import su.afk.yummy.tv.feature.details.model.DetailsWatchProgressIndex

class DetailsState {
    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val details: AnimeDetails? = null,
        val videosState: VideosUiState = VideosUiState.NotLoaded,
        val error: String? = null,
        val isInLibrary: Boolean = false,
        val isFavorite: Boolean = false,
        val libraryList: UserAnimeList? = null,
        val watchProgress: DetailsWatchProgressIndex = DetailsWatchProgressIndex.Empty,
        val pendingBalancerSelection: BalancerPickerState? = null,
        val pendingDubbingSelection: DubbingPickerState? = null,
        val showLibraryListPicker: Boolean = false,
        val isWatchLaunchPending: Boolean = false,
        val isSignedIn: Boolean = false,
        val subscriptions: ImmutableList<SubscriptionOption> = persistentListOf(),
        val showSubscriptionsPicker: Boolean = false,
        val isSubscriptionsLoading: Boolean = false,
        val detailsButtonOrder: ImmutableList<DetailsButtonAction> =
            SettingsStore.defaultDetailsButtonOrder.toImmutableList(),
    ) : UiState

    /** Пользовательские действия на основном экране деталей аниме. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь запросил повторную загрузку деталей. */
        data object RetrySelected : Event

        /** Пользователь выбрал запуск просмотра. */
        data object WatchSelected : Event

        /** Пользователь выбрал связанное аниме с указанным идентификатором. */
        data class AnimeSelected(val seriesId: Int) : Event

        /** Пользователь открыл полный текст деталей. */
        data object FullDetailsSelected : Event

        /** Пользователь открыл список эпизодов. */
        data object EpisodesSelected : Event

        /** Пользователь открыл трейлеры. */
        data object TrailersSelected : Event

        /** Пользователь открыл похожие тайтлы. */
        data object SimilarSelected : Event

        /** Пользователь открыл порядок просмотра. */
        data object ViewingOrderSelected : Event

        /** Пользователь открыл скриншоты. */
        data object ScreenshotsSelected : Event

        /** Пользователь открыл экран рейтинга. */
        data object RatingScreenSelected : Event

        /** Пользователь открыл коллекции с этим аниме. */
        data object CollectionsSelected : Event

        /** Пользователь открыл комментарии к этому аниме. */
        data object CommentsSelected : Event

        data object ReviewsSelected : Event

        data object BloggerVideosSelected : Event

        /** Пользователь переключил наличие тайтла в библиотеке. */
        data object LibraryToggled : Event

        /** Пользователь переключил признак избранного. */
        data object FavoriteToggled : Event

        /** Пользователь закрыл выбор списка библиотеки. */
        data object LibraryListPickerDismissed : Event

        /** Пользователь выбрал список библиотеки для тайтла. */
        data class LibraryListSelected(val list: UserAnimeList) : Event

        /** Пользователь открыл постер на весь экран. */
        data object PosterClicked : Event

        /** Пользователь закрыл выбор балансера. */
        data object BalancerPickerDismissed : Event

        /** Пользователь подтвердил видео для запуска после выбора балансера. */
        data class BalancerConfirmed(val video: AnimeVideo) : Event

        /** Пользователь выбрал озвучку в диалоге выбора озвучки. */
        data class DubbingSelected(val video: AnimeVideo) : Event

        /** Пользователь закрыл диалог выбора озвучки. */
        data object DubbingPickerDismissed : Event

        /** Пользователь открыл отдельный экран подписок. */
        data object SubscriptionsRouteSelected : Event

        /** Пользователь открыл быстрый выбор подписок. */
        data object SubscriptionsSelected : Event

        /** Пользователь закрыл быстрый выбор подписок. */
        data object SubscriptionsDismissed : Event

        /** Пользователь переключил подписку с указанным ключом. */
        data class SubscriptionToggled(val key: String) : Event
    }

    sealed interface Effect : UiEffect
}
