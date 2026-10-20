package su.afk.yummy.tv.feature.details.details

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeTrailer
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.settings.DetailsButtonAction
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.details.model.BalancerPickerState
import su.afk.yummy.tv.feature.details.details.model.DetailsRatingOverview
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
        val error: UiText? = null,
        val isInLibrary: Boolean = false,
        val isFavorite: Boolean = false,
        val libraryList: UserAnimeList? = null,
        /** Оценка пользователя (1..10); `null`, если тайтл не оценён. */
        val userRating: Int? = null,
        /** Трейлеры тайтла; пусто, пока не загрузились или их нет. */
        val trailers: ImmutableList<AnimeTrailer> = persistentListOf(),
        /** Есть ли видео блогеров по тайтлу; карточка скрыта, когда их точно нет. */
        val hasBloggerVideos: Boolean = true,
        /** Сводка рейтинга и статистики списков; `null`, если показывать нечего. */
        val ratingOverview: DetailsRatingOverview? = null,
        /** Трейлеры и сводка рейтинга ещё грузятся — блоки показывают скелетоны. */
        val isExtrasLoading: Boolean = true,
        val watchProgress: DetailsWatchProgressIndex = DetailsWatchProgressIndex.Empty,
        val pendingBalancerSelection: BalancerPickerState? = null,
        val pendingDubbingSelection: DubbingPickerState? = null,
        val showLibraryListPicker: Boolean = false,
        val isWatchLaunchPending: Boolean = false,
        val isSignedIn: Boolean = false,
        val subscriptions: ImmutableList<SubscriptionOption> = persistentListOf(),
        val showSubscriptionsPicker: Boolean = false,
        val isSubscriptionsLoading: Boolean = false,
        /** Гостю показана шторка «войдите, чтобы получать уведомления о новых сериях». */
        val showSubscriptionsSignInPrompt: Boolean = false,
        /** Открыта шторка с другими названиями тайтла (кнопка (i) у названия). */
        val showInfoSheet: Boolean = false,
        /** Системный запрос разрешения на уведомления уже показывали. */
        val notificationPermissionRequested: Boolean = false,
        val detailsButtonOrder: ImmutableList<DetailsButtonAction> =
            SettingsStore.defaultDetailsButtonOrder.toImmutableList(),
    ) : UiState

    /** Пользовательские действия на основном экране деталей аниме. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** «Поделиться»: ссылка на страницу тайтла на сайте. */
        data object ShareSelected : Event

        /** Пользователь запросил повторную загрузку деталей. */
        data object RetrySelected : Event

        /** Пользователь выбрал запуск просмотра. */
        data object WatchSelected : Event

        /** Пользователь выбрал связанное аниме с указанным идентификатором. */
        data class AnimeSelected(val seriesId: Int) : Event

        /** Пользователь открыл полный текст деталей. */
        data object FullDetailsSelected : Event

        /** Пользователь нажал (i) у названия — открыть шторку с другими названиями. */
        data object InfoSelected : Event

        /** Пользователь закрыл шторку с другими названиями. */
        data object InfoDismissed : Event

        /** Пользователь нажал на жанр. */
        data class GenreSelected(val id: Int) : Event

        /** Пользователь нажал на студию. */
        data class StudioSelected(val id: Int, val url: String?) : Event

        /** Пользователь нажал на создателя. */
        data class DirectorSelected(val id: Int) : Event

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

        /** Пользователь нажал на скриншот с указанным индексом в ленте деталей. */
        data class ScreenshotSelected(val index: Int) : Event

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

        /** Гость нажал на заблокированный пункт «Подписки». */
        data object SubscriptionsSignInPromptSelected : Event

        /** Гость закрыл шторку с предложением войти. */
        data object SubscriptionsSignInPromptDismissed : Event

        /** Гость нажал «Войти» в шторке подписок — открываем вкладку профиля. */
        data object SignInSelected : Event

        /** Экран показал системный запрос разрешения на уведомления. */
        data object NotificationPermissionRequested : Event
    }

    sealed interface Effect : UiEffect {
        /** Открыть системный шаринг со ссылкой [url] на тайтл [title]. */
        data class ShareAnime(val title: String, val url: String) : Effect
    }
}
