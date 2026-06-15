package su.afk.yummy.tv.feature.account

import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import javax.inject.Inject

internal class AccountAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Пользователь выбрал вкладку аккаунта.
     *
     * Параметры: tab.
     */
    fun eventTabSelected(tab: AccountState.AccountTab) {
        tracker.track(EVENT_TAB_SELECTED, analyticsParamsOf(PARAM_TAB to tab.name.lowercase()))
    }

    /**
     * Пользователь нажал вход в аккаунт.
     */
    fun eventLoginSelected() {
        tracker.track(EVENT_LOGIN_SELECTED)
    }

    /**
     * Пользователь нажал выход из аккаунта.
     */
    fun eventLogoutSelected() {
        tracker.track(EVENT_LOGOUT_SELECTED)
    }

    /**
     * Пользователь запросил обновление профиля.
     */
    fun eventRefreshProfileSelected() {
        tracker.track(EVENT_REFRESH_PROFILE_SELECTED)
    }

    /**
     * Пользователь запросил обновление блока аккаунта со статистикой и уведомлениями.
     */
    fun eventRefreshHubSelected() {
        tracker.track(EVENT_REFRESH_HUB_SELECTED)
    }

    /**
     * Пользователь открыл уведомление о новом эпизоде.
     *
     * Параметры: notification_id.
     */
    fun eventNotificationSelected(notificationId: Int) {
        tracker.track(
            EVENT_NOTIFICATION_SELECTED,
            analyticsParamsOf(PARAM_NOTIFICATION_ID to notificationId)
        )
    }

    /**
     * Пользователь отметил уведомление прочитанным.
     *
     * Параметры: notification_id.
     */
    fun eventNotificationReadSelected(notificationId: Int) {
        tracker.track(
            EVENT_NOTIFICATION_READ_SELECTED,
            analyticsParamsOf(PARAM_NOTIFICATION_ID to notificationId)
        )
    }

    /**
     * Пользователь отметил все уведомления прочитанными.
     */
    fun eventAllNotificationsReadSelected() {
        tracker.track(EVENT_ALL_NOTIFICATIONS_READ_SELECTED)
    }

    /**
     * Пользователь удалил уведомление.
     *
     * Параметры: notification_id.
     */
    fun eventNotificationDeleteSelected(notificationId: Int) {
        tracker.track(
            EVENT_NOTIFICATION_DELETE_SELECTED,
            analyticsParamsOf(PARAM_NOTIFICATION_ID to notificationId)
        )
    }

    internal companion object {
        private const val PARAM_NOTIFICATION_ID = "notification_id"
        private const val PARAM_TAB = "tab"

        const val EVENT_TAB_SELECTED = "account_tab_selected"
        const val EVENT_LOGIN_SELECTED = "account_login_selected"
        const val EVENT_LOGOUT_SELECTED = "account_logout_selected"
        const val EVENT_REFRESH_PROFILE_SELECTED = "account_refresh_profile_selected"
        const val EVENT_REFRESH_HUB_SELECTED = "account_refresh_hub_selected"
        const val EVENT_NOTIFICATION_SELECTED = "account_notification_selected"
        const val EVENT_NOTIFICATION_READ_SELECTED = "account_notification_read_selected"
        const val EVENT_ALL_NOTIFICATIONS_READ_SELECTED = "account_all_notifications_read_selected"
        const val EVENT_NOTIFICATION_DELETE_SELECTED = "account_notification_delete_selected"
    }
}
