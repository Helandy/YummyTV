package su.afk.yummy.tv.feature.details

import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import javax.inject.Inject

internal class DetailsAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Пользователь открыл экран деталей.
     *
     * Параметры: anime_id.
     */
    fun eventDetailsScreenOpened(animeId: Int) {
        eventWithAnime(EVENT_DETAILS_SCREEN_OPENED, animeId)
    }

    /**
     * Пользователь открыл полный текст описания как отдельный экран.
     *
     * Параметры: anime_id.
     */
    fun eventFullScreenOpened(animeId: Int) {
        eventWithAnime(EVENT_FULL_SCREEN_OPENED, animeId)
    }

    /**
     * Пользователь открыл список эпизодов как отдельный экран.
     *
     * Параметры: anime_id.
     */
    fun eventEpisodesScreenOpened(animeId: Int) {
        eventWithAnime(EVENT_EPISODES_SCREEN_OPENED, animeId)
    }

    /**
     * Пользователь открыл озвучки эпизода как отдельный экран.
     *
     * Параметры: anime_id.
     */
    fun eventEpisodeDubbingsScreenOpened(animeId: Int) {
        eventWithAnime(EVENT_EPISODE_DUBBINGS_SCREEN_OPENED, animeId)
    }

    /**
     * Пользователь открыл подписки как отдельный экран.
     *
     * Параметры: anime_id.
     */
    fun eventSubscriptionsScreenOpened(animeId: Int) {
        eventWithAnime(EVENT_SUBSCRIPTIONS_SCREEN_OPENED, animeId)
    }

    /**
     * Пользователь открыл трейлеры как отдельный экран.
     *
     * Параметры: anime_id.
     */
    fun eventTrailersScreenOpened(animeId: Int) {
        eventWithAnime(EVENT_TRAILERS_SCREEN_OPENED, animeId)
    }

    /**
     * Пользователь открыл похожие аниме как отдельный экран.
     *
     * Параметры: anime_id.
     */
    fun eventSimilarScreenOpened(animeId: Int) {
        eventWithAnime(EVENT_SIMILAR_SCREEN_OPENED, animeId)
    }

    /**
     * Пользователь открыл порядок просмотра как отдельный экран.
     *
     * Параметры: anime_id.
     */
    fun eventViewingOrderScreenOpened(animeId: Int) {
        eventWithAnime(EVENT_VIEWING_ORDER_SCREEN_OPENED, animeId)
    }

    /**
     * Пользователь открыл скриншоты как отдельный экран.
     *
     * Параметры: anime_id.
     */
    fun eventScreenshotsScreenOpened(animeId: Int) {
        eventWithAnime(EVENT_SCREENSHOTS_SCREEN_OPENED, animeId)
    }

    /**
     * Пользователь открыл оценку как отдельный экран.
     *
     * Параметры: anime_id.
     */
    fun eventRatingScreenOpened(animeId: Int) {
        eventWithAnime(EVENT_RATING_SCREEN_OPENED, animeId)
    }

    /**
     * Пользователь открыл коллекции с аниме как отдельный экран.
     *
     * Параметры: anime_id.
     */
    fun eventCollectionsScreenOpened(animeId: Int) {
        eventWithAnime(EVENT_COLLECTIONS_SCREEN_OPENED, animeId)
    }

    /**
     * Пользователь повторил загрузку экрана деталей.
     *
     * Параметры: anime_id.
     */
    fun eventDetailsRetry(animeId: Int) {
        eventWithAnime(EVENT_DETAILS_RETRY, animeId)
    }

    /**
     * Пользователь нажал основную кнопку просмотра на экране деталей.
     *
     * Параметры: anime_id.
     */
    fun eventDetailsWatchSelected(animeId: Int) {
        eventWithAnime(EVENT_DETAILS_WATCH_SELECTED, animeId)
    }

    /**
     * Пользователь выбрал связанное аниме на экране деталей.
     *
     * Параметры: anime_id, target_anime_id.
     */
    fun eventDetailsAnimeSelected(animeId: Int, targetAnimeId: Int) {
        eventWithAnime(
            eventName = EVENT_DETAILS_ANIME_SELECTED,
            animeId = animeId,
            params = analyticsParamsOf(PARAM_TARGET_ANIME_ID to targetAnimeId),
        )
    }

    /**
     * Пользователь подтвердил выбранный балансер на экране деталей.
     *
     * Параметры: anime_id, video_id.
     */
    fun eventDetailsBalancerConfirmed(animeId: Int, videoId: Int) {
        eventWithAnime(
            eventName = EVENT_DETAILS_BALANCER_CONFIRMED,
            animeId = animeId,
            params = analyticsParamsOf(PARAM_VIDEO_ID to videoId),
        )
    }

    /**
     * Пользователь открыл полный текст описания.
     *
     * Параметры: anime_id.
     */
    fun eventDetailsFullDetailsSelected(animeId: Int) {
        eventWithAnime(EVENT_DETAILS_FULL_DETAILS_SELECTED, animeId)
    }

    /**
     * Пользователь открыл список эпизодов.
     *
     * Параметры: anime_id.
     */
    fun eventDetailsEpisodesSelected(animeId: Int) {
        eventWithAnime(EVENT_DETAILS_EPISODES_SELECTED, animeId)
    }

    /**
     * Пользователь открыл трейлеры.
     *
     * Параметры: anime_id.
     */
    fun eventDetailsTrailersSelected(animeId: Int) {
        eventWithAnime(EVENT_DETAILS_TRAILERS_SELECTED, animeId)
    }

    /**
     * Пользователь открыл похожие аниме.
     *
     * Параметры: anime_id.
     */
    fun eventDetailsSimilarSelected(animeId: Int) {
        eventWithAnime(EVENT_DETAILS_SIMILAR_SELECTED, animeId)
    }

    /**
     * Пользователь открыл порядок просмотра.
     *
     * Параметры: anime_id.
     */
    fun eventDetailsViewingOrderSelected(animeId: Int) {
        eventWithAnime(EVENT_DETAILS_VIEWING_ORDER_SELECTED, animeId)
    }

    /**
     * Пользователь открыл скриншоты.
     *
     * Параметры: anime_id.
     */
    fun eventDetailsScreenshotsSelected(animeId: Int) {
        eventWithAnime(EVENT_DETAILS_SCREENSHOTS_SELECTED, animeId)
    }

    /**
     * Пользователь открыл экран оценки.
     *
     * Параметры: anime_id.
     */
    fun eventDetailsRatingScreenSelected(animeId: Int) {
        eventWithAnime(EVENT_DETAILS_RATING_SCREEN_SELECTED, animeId)
    }

    /**
     * Пользователь открыл коллекции с этим аниме.
     *
     * Параметры: anime_id.
     */
    fun eventDetailsCollectionsSelected(animeId: Int) {
        eventWithAnime(EVENT_DETAILS_COLLECTIONS_SELECTED, animeId)
    }

    /**
     * Пользователь включил или выключил нахождение аниме в библиотеке.
     *
     * Параметры: anime_id, target_state.
     */
    fun eventDetailsLibraryToggled(animeId: Int, targetState: Boolean) {
        eventWithAnime(
            eventName = EVENT_DETAILS_LIBRARY_TOGGLED,
            animeId = animeId,
            params = analyticsParamsOf(PARAM_TARGET_STATE to targetState),
        )
    }

    /**
     * Пользователь включил или выключил избранное.
     *
     * Параметры: anime_id, target_state.
     */
    fun eventDetailsFavoriteToggled(animeId: Int, targetState: Boolean) {
        eventWithAnime(
            eventName = EVENT_DETAILS_FAVORITE_TOGGLED,
            animeId = animeId,
            params = analyticsParamsOf(PARAM_TARGET_STATE to targetState),
        )
    }

    /**
     * Пользователь выбрал список библиотеки для аниме.
     *
     * Параметры: anime_id, list.
     */
    fun eventDetailsLibraryListSelected(animeId: Int, list: UserAnimeList) {
        eventWithAnime(
            eventName = EVENT_DETAILS_LIBRARY_LIST_SELECTED,
            animeId = animeId,
            params = analyticsParamsOf(PARAM_LIST to list.name.lowercase()),
        )
    }

    /**
     * Пользователь открыл постер на весь экран.
     *
     * Параметры: anime_id.
     */
    fun eventDetailsPosterClicked(animeId: Int) {
        eventWithAnime(EVENT_DETAILS_POSTER_CLICKED, animeId)
    }

    /**
     * Пользователь выбрал коллекцию на экране деталей.
     *
     * Параметры: anime_id, collection_id.
     */
    fun eventDetailsCollectionSelected(animeId: Int, collectionId: Int) {
        eventWithAnime(
            eventName = EVENT_DETAILS_COLLECTION_SELECTED,
            animeId = animeId,
            params = analyticsParamsOf(PARAM_COLLECTION_ID to collectionId),
        )
    }

    /**
     * Пользователь открыл отдельный экран подписок.
     *
     * Параметры: anime_id.
     */
    fun eventDetailsSubscriptionsRouteSelected(animeId: Int) {
        eventWithAnime(EVENT_DETAILS_SUBSCRIPTIONS_ROUTE_SELECTED, animeId)
    }

    /**
     * Пользователь открыл выбор подписок внутри экрана деталей.
     *
     * Параметры: anime_id.
     */
    fun eventDetailsSubscriptionsSelected(animeId: Int) {
        eventWithAnime(EVENT_DETAILS_SUBSCRIPTIONS_SELECTED, animeId)
    }

    /**
     * Пользователь изменил подписку внутри экрана деталей.
     *
     * Параметры: anime_id, video_id, target_state.
     */
    fun eventDetailsSubscriptionToggled(animeId: Int, videoId: Int, targetState: Boolean) {
        eventWithAnime(
            eventName = EVENT_DETAILS_SUBSCRIPTION_TOGGLED,
            animeId = animeId,
            params = analyticsParamsOf(
                PARAM_VIDEO_ID to videoId,
                PARAM_TARGET_STATE to targetState,
            ),
        )
    }

    /**
     * Пользователь повторил загрузку коллекций аниме.
     *
     * Параметры: anime_id.
     */
    fun eventCollectionsRetry(animeId: Int) {
        eventWithAnime(EVENT_COLLECTIONS_RETRY, animeId)
    }

    /**
     * Пользователь выбрал коллекцию со страницы коллекций аниме.
     *
     * Параметры: anime_id, collection_id.
     */
    fun eventCollectionsCollectionSelected(animeId: Int, collectionId: Int) {
        eventWithAnime(
            eventName = EVENT_COLLECTIONS_COLLECTION_SELECTED,
            animeId = animeId,
            params = analyticsParamsOf(PARAM_COLLECTION_ID to collectionId),
        )
    }

    /**
     * Пользователь повторил загрузку экрана подписок.
     *
     * Параметры: anime_id.
     */
    fun eventSubscriptionsRetry(animeId: Int) {
        eventWithAnime(EVENT_SUBSCRIPTIONS_RETRY, animeId)
    }

    /**
     * Пользователь изменил подписку на отдельном экране подписок.
     *
     * Параметры: anime_id, video_id, target_state.
     */
    fun eventSubscriptionsSubscriptionToggled(animeId: Int, videoId: Int, targetState: Boolean) {
        eventWithAnime(
            eventName = EVENT_SUBSCRIPTIONS_SUBSCRIPTION_TOGGLED,
            animeId = animeId,
            params = analyticsParamsOf(
                PARAM_VIDEO_ID to videoId,
                PARAM_TARGET_STATE to targetState,
            ),
        )
    }

    /**
     * Пользователь открыл озвучки конкретного эпизода.
     *
     * Параметры: anime_id.
     */
    fun eventEpisodesEpisodeDubbingsSelected(animeId: Int) {
        eventWithAnime(EVENT_EPISODES_EPISODE_DUBBINGS_SELECTED, animeId)
    }

    /**
     * Пользователь выбрал видео на экране эпизодов.
     *
     * Параметры: anime_id, video_id.
     */
    fun eventEpisodesVideoSelected(animeId: Int, videoId: Int) {
        eventWithAnime(
            eventName = EVENT_EPISODES_VIDEO_SELECTED,
            animeId = animeId,
            params = analyticsParamsOf(PARAM_VIDEO_ID to videoId),
        )
    }

    /**
     * Пользователь подтвердил балансер на экране эпизодов.
     *
     * Параметры: anime_id, video_id.
     */
    fun eventEpisodesBalancerConfirmed(animeId: Int, videoId: Int) {
        eventWithAnime(
            eventName = EVENT_EPISODES_BALANCER_CONFIRMED,
            animeId = animeId,
            params = analyticsParamsOf(PARAM_VIDEO_ID to videoId),
        )
    }

    /**
     * Пользователь повторил загрузку полного описания.
     *
     * Параметры: anime_id.
     */
    fun eventFullRetry(animeId: Int) {
        eventWithAnime(EVENT_FULL_RETRY, animeId)
    }

    /**
     * Пользователь выбрал аниме из порядка просмотра.
     *
     * Параметры: anime_id, target_anime_id.
     */
    fun eventViewingOrderAnimeSelected(animeId: Int, targetAnimeId: Int) {
        eventWithAnime(
            eventName = EVENT_VIEWING_ORDER_ANIME_SELECTED,
            animeId = animeId,
            params = analyticsParamsOf(PARAM_TARGET_ANIME_ID to targetAnimeId),
        )
    }

    /**
     * Пользователь выбрал похожее аниме.
     *
     * Параметры: anime_id, target_anime_id.
     */
    fun eventSimilarAnimeSelected(animeId: Int, targetAnimeId: Int) {
        eventWithAnime(
            eventName = EVENT_SIMILAR_ANIME_SELECTED,
            animeId = animeId,
            params = analyticsParamsOf(PARAM_TARGET_ANIME_ID to targetAnimeId),
        )
    }

    /**
     * Пользователь переключил источник похожих аниме.
     *
     * Параметры: anime_id, from_ai.
     */
    fun eventSimilarSourceSelected(animeId: Int, fromAi: Boolean) {
        eventWithAnime(
            eventName = EVENT_SIMILAR_SOURCE_SELECTED,
            animeId = animeId,
            params = analyticsParamsOf(PARAM_FROM_AI to fromAi),
        )
    }

    /**
     * Пользователь выбрал скриншот.
     *
     * Параметры: anime_id, selected_index.
     */
    fun eventScreenshotsScreenshotSelected(animeId: Int, selectedIndex: Int) {
        eventWithAnime(
            eventName = EVENT_SCREENSHOTS_SCREENSHOT_SELECTED,
            animeId = animeId,
            params = analyticsParamsOf(PARAM_SELECTED_INDEX to selectedIndex),
        )
    }

    /**
     * Пользователь перешел к предыдущему скриншоту.
     *
     * Параметры: anime_id.
     */
    fun eventScreenshotsPreviousSelected(animeId: Int) {
        eventWithAnime(EVENT_SCREENSHOTS_PREVIOUS_SELECTED, animeId)
    }

    /**
     * Пользователь перешел к следующему скриншоту.
     *
     * Параметры: anime_id.
     */
    fun eventScreenshotsNextSelected(animeId: Int) {
        eventWithAnime(EVENT_SCREENSHOTS_NEXT_SELECTED, animeId)
    }

    /**
     * Пользователь повторил загрузку экрана оценки.
     *
     * Параметры: anime_id.
     */
    fun eventRatingRetry(animeId: Int) {
        eventWithAnime(EVENT_RATING_RETRY, animeId)
    }

    /**
     * Пользователь выбрал оценку аниме.
     *
     * Параметры: anime_id, rating.
     */
    fun eventRatingSelected(animeId: Int, rating: Int) {
        eventWithAnime(
            eventName = EVENT_RATING_RATING_SELECTED,
            animeId = animeId,
            params = analyticsParamsOf(PARAM_RATING to rating),
        )
    }

    /**
     * Пользователь удалил свою оценку аниме.
     *
     * Параметры: anime_id.
     */
    fun eventRatingDeleted(animeId: Int) {
        eventWithAnime(EVENT_RATING_RATING_DELETED, animeId)
    }

    private fun eventWithAnime(
        eventName: String,
        animeId: Int,
        params: Map<String, String> = emptyMap(),
    ) {
        tracker.track(
            eventName,
            analyticsParamsOf(PARAM_ANIME_ID to animeId) + params,
        )
    }

    internal companion object {
        private const val PARAM_ANIME_ID = "anime_id"
        private const val PARAM_COLLECTION_ID = "collection_id"
        private const val PARAM_FROM_AI = "from_ai"
        private const val PARAM_LIST = "list"
        private const val PARAM_RATING = "rating"
        private const val PARAM_SELECTED_INDEX = "selected_index"
        private const val PARAM_TARGET_ANIME_ID = "target_anime_id"
        private const val PARAM_TARGET_STATE = "target_state"
        private const val PARAM_VIDEO_ID = "video_id"

        const val EVENT_DETAILS_SCREEN_OPENED = "details_screen"

        const val EVENT_DETAILS_RETRY = "details_retry"

        const val EVENT_DETAILS_WATCH_SELECTED = "details_watch_selected"

        const val EVENT_DETAILS_ANIME_SELECTED = "details_anime_selected"

        const val EVENT_DETAILS_BALANCER_CONFIRMED = "details_balancer_confirmed"

        const val EVENT_DETAILS_FULL_DETAILS_SELECTED = "details_full_details_selected"

        const val EVENT_DETAILS_EPISODES_SELECTED = "details_episodes_selected"

        const val EVENT_DETAILS_TRAILERS_SELECTED = "details_trailers_selected"

        const val EVENT_DETAILS_SIMILAR_SELECTED = "details_similar_selected"

        const val EVENT_DETAILS_VIEWING_ORDER_SELECTED = "details_viewing_order_selected"

        const val EVENT_DETAILS_SCREENSHOTS_SELECTED = "details_screenshots_selected"

        const val EVENT_DETAILS_RATING_SCREEN_SELECTED = "details_rating_screen_selected"

        const val EVENT_DETAILS_COLLECTIONS_SELECTED = "details_collections_selected"

        const val EVENT_DETAILS_LIBRARY_TOGGLED = "details_library_toggled"

        const val EVENT_DETAILS_FAVORITE_TOGGLED = "details_favorite_toggled"

        const val EVENT_DETAILS_LIBRARY_LIST_SELECTED = "details_library_list_selected"

        const val EVENT_DETAILS_POSTER_CLICKED = "details_poster_clicked"

        const val EVENT_DETAILS_COLLECTION_SELECTED = "details_collection_selected"

        const val EVENT_DETAILS_SUBSCRIPTIONS_ROUTE_SELECTED =
            "details_subscriptions_route_selected"

        const val EVENT_DETAILS_SUBSCRIPTIONS_SELECTED = "details_subscriptions_selected"

        const val EVENT_DETAILS_SUBSCRIPTION_TOGGLED = "details_subscription_toggled"

        const val EVENT_COLLECTIONS_RETRY = "details_collections_retry"

        const val EVENT_COLLECTIONS_COLLECTION_SELECTED = "details_collections_collection_selected"

        const val EVENT_SUBSCRIPTIONS_RETRY = "details_subscriptions_retry"

        const val EVENT_SUBSCRIPTIONS_SUBSCRIPTION_TOGGLED =
            "details_subscriptions_subscription_toggled"

        const val EVENT_EPISODES_SCREEN_OPENED = "details_episodes_screen"

        const val EVENT_EPISODE_DUBBINGS_SCREEN_OPENED = "details_episode_dubbings_screen"

        const val EVENT_EPISODES_EPISODE_DUBBINGS_SELECTED =
            "details_episodes_episode_dubbings_selected"

        const val EVENT_EPISODES_VIDEO_SELECTED = "details_episodes_video_selected"

        const val EVENT_EPISODES_BALANCER_CONFIRMED = "details_episodes_balancer_confirmed"

        const val EVENT_FULL_SCREEN_OPENED = "details_full_screen"

        const val EVENT_FULL_RETRY = "details_full_retry"

        const val EVENT_VIEWING_ORDER_SCREEN_OPENED = "details_viewing_order_screen"

        const val EVENT_VIEWING_ORDER_ANIME_SELECTED = "details_viewing_order_anime_selected"

        const val EVENT_SIMILAR_SCREEN_OPENED = "details_similar_screen"

        const val EVENT_SIMILAR_ANIME_SELECTED = "details_similar_anime_selected"

        const val EVENT_SIMILAR_SOURCE_SELECTED = "details_similar_source_selected"

        const val EVENT_SCREENSHOTS_SCREEN_OPENED = "details_screenshots_screen"

        const val EVENT_SCREENSHOTS_SCREENSHOT_SELECTED = "details_screenshots_screenshot_selected"

        const val EVENT_SCREENSHOTS_PREVIOUS_SELECTED = "details_screenshots_previous_selected"

        const val EVENT_SCREENSHOTS_NEXT_SELECTED = "details_screenshots_next_selected"

        const val EVENT_RATING_SCREEN_OPENED = "details_rating_screen"

        const val EVENT_RATING_RETRY = "details_rating_retry"

        const val EVENT_RATING_RATING_SELECTED = "details_rating_rating_selected"

        const val EVENT_RATING_RATING_DELETED = "details_rating_rating_deleted"

        const val EVENT_SUBSCRIPTIONS_SCREEN_OPENED = "details_subscriptions_screen"

        const val EVENT_TRAILERS_SCREEN_OPENED = "details_trailers_screen"

        const val EVENT_COLLECTIONS_SCREEN_OPENED = "details_collections_screen"
    }
}
