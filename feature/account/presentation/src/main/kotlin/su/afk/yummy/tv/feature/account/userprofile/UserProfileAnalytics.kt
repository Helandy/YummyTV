package su.afk.yummy.tv.feature.account.userprofile

import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import javax.inject.Inject

internal class UserProfileAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Пользователь открыл публичный профиль.
     *
     * Параметры: user_id.
     */
    fun eventScreenOpened(userId: Int) {
        eventWithUser(EVENT_SCREEN_OPENED, userId)
    }

    /**
     * Пользователь повторил загрузку обзора профиля.
     *
     * Параметры: user_id.
     */
    fun eventRetryOverviewSelected(userId: Int) {
        eventWithUser(EVENT_RETRY_OVERVIEW_SELECTED, userId)
    }

    /**
     * Пользователь выбрал вкладку публичного профиля.
     *
     * Параметры: user_id, tab.
     */
    fun eventTabSelected(userId: Int, tab: UserProfileState.Tab) {
        tracker.track(
            EVENT_TAB_SELECTED,
            userParams(userId) + analyticsParamsOf(PARAM_TAB to tab.analyticsValue()),
        )
    }

    /**
     * Пользователь выбрал фильтр списка аниме в публичном профиле.
     *
     * Параметры: user_id, filter.
     */
    fun eventListFilterSelected(userId: Int, filter: UserProfileState.ListFilter) {
        tracker.track(
            EVENT_LIST_FILTER_SELECTED,
            userParams(userId) + analyticsParamsOf(PARAM_FILTER to filter.analyticsValue()),
        )
    }

    /**
     * Пользователь повторил загрузку текущей вкладки.
     *
     * Параметры: user_id, tab.
     */
    fun eventRetryTabSelected(userId: Int, tab: UserProfileState.Tab) {
        tracker.track(
            EVENT_RETRY_TAB_SELECTED,
            userParams(userId) + analyticsParamsOf(PARAM_TAB to tab.analyticsValue()),
        )
    }

    /**
     * Пользователь открыл аниме из публичного профиля.
     *
     * Параметры: user_id, anime_id.
     */
    fun eventAnimeSelected(userId: Int, animeId: Int) {
        tracker.track(
            EVENT_ANIME_SELECTED,
            userParams(userId) + analyticsParamsOf(PARAM_ANIME_ID to animeId),
        )
    }

    /**
     * Пользователь открыл друга из публичного профиля.
     *
     * Параметры: user_id, target_user_id.
     */
    fun eventFriendSelected(userId: Int, targetUserId: Int) {
        tracker.track(
            EVENT_FRIEND_SELECTED,
            userParams(userId) + analyticsParamsOf(PARAM_TARGET_USER_ID to targetUserId),
        )
    }

    /**
     * Ошибка загрузки обзора публичного профиля.
     */
    fun eventOverviewLoadError(userId: Int, throwable: Throwable) {
        tracker.reportError(
            groupIdentifier = ERROR_OVERVIEW_LOAD,
            message = "$ERROR_OVERVIEW_LOAD_MESSAGE_PREFIX ($PARAM_USER_ID=$userId): " +
                    throwable.analyticsType(),
            throwable = throwable,
        )
    }

    /**
     * Ошибка загрузки вкладки публичного профиля.
     */
    fun eventTabLoadError(userId: Int, tab: UserProfileState.Tab, throwable: Throwable) {
        tracker.reportError(
            groupIdentifier = ERROR_TAB_LOAD,
            message = "$ERROR_TAB_LOAD_MESSAGE_PREFIX ($PARAM_USER_ID=$userId, " +
                    "$PARAM_TAB=${tab.analyticsValue()}): ${throwable.analyticsType()}",
            throwable = throwable,
        )
    }

    private fun eventWithUser(eventName: String, userId: Int) {
        tracker.track(eventName, userParams(userId))
    }

    private fun userParams(userId: Int): Map<String, String> =
        analyticsParamsOf(PARAM_USER_ID to userId)

    private fun UserProfileState.Tab.analyticsValue(): String = name.lowercase()

    private fun UserProfileState.ListFilter.analyticsValue(): String = name.lowercase()

    private fun Throwable.analyticsType(): String =
        this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "unknown"

    internal companion object {
        private const val ERROR_OVERVIEW_LOAD = "user_profile_overview_load_error"
        private const val ERROR_OVERVIEW_LOAD_MESSAGE_PREFIX = "User profile overview load failed"
        private const val ERROR_TAB_LOAD = "user_profile_tab_load_error"
        private const val ERROR_TAB_LOAD_MESSAGE_PREFIX = "User profile tab load failed"

        private const val PARAM_ANIME_ID = "anime_id"
        private const val PARAM_FILTER = "filter"
        private const val PARAM_TAB = "tab"
        private const val PARAM_TARGET_USER_ID = "target_user_id"
        private const val PARAM_USER_ID = "user_id"

        const val EVENT_ANIME_SELECTED = "user_profile_anime_selected"
        const val EVENT_FRIEND_SELECTED = "user_profile_friend_selected"
        const val EVENT_LIST_FILTER_SELECTED = "user_profile_list_filter_selected"
        const val EVENT_RETRY_OVERVIEW_SELECTED = "user_profile_retry_overview_selected"
        const val EVENT_RETRY_TAB_SELECTED = "user_profile_retry_tab_selected"
        const val EVENT_SCREEN_OPENED = "user_profile_screen"
        const val EVENT_TAB_SELECTED = "user_profile_tab_selected"
    }
}
