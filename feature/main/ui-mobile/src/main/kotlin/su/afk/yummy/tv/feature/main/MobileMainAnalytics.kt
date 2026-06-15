package su.afk.yummy.tv.feature.main

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.analytics.AnalyticsDestination
import su.afk.yummy.tv.core.analytics.AnalyticsEvents
import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.core.navigation.root.RootTab
import javax.inject.Inject

internal class MobileMainAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Пользователь увидел экран мобильного приложения.
     *
     * Параметры: screen и параметры видимого destination.
     */
    fun eventScreenView(destination: NavKey) {
        val analyticsDestination = destination as? AnalyticsDestination ?: return
        tracker.track(
            AnalyticsEvents.screenView(
                screenName = analyticsDestination.screenName,
                params = analyticsDestination.screenParams,
            )
        )
    }

    /**
     * Пользователь открыл настройки из мобильной главной навигации.
     */
    fun eventSettingsSelected() {
        tracker.track(EVENT_SETTINGS_SELECTED)
    }

    /**
     * Пользователь открыл аккаунт из мобильной главной навигации.
     */
    fun eventAccountSelected() {
        tracker.track(EVENT_ACCOUNT_SELECTED)
    }

    /**
     * Пользователь выбрал корневой раздел мобильной навигации.
     *
     * Параметры: root.
     */
    fun eventRootSelected(root: RootTab) {
        tracker.track(EVENT_ROOT_SELECTED, analyticsParamsOf(PARAM_ROOT to root.name.lowercase()))
    }

    internal companion object {
        private const val PARAM_ROOT = "root"

        const val EVENT_SETTINGS_SELECTED = "main_settings_selected"
        const val EVENT_ACCOUNT_SELECTED = "main_account_selected"
        const val EVENT_ROOT_SELECTED = "main_root_selected"
    }
}
