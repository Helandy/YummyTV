package su.afk.yummy.tv.feature.settings

import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.core.preferences.settings.AppTheme
import su.afk.yummy.tv.core.preferences.settings.DetailsButtonAction
import su.afk.yummy.tv.core.preferences.settings.PosterCardSize
import su.afk.yummy.tv.core.preferences.settings.PosterQuality
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.core.preferences.settings.PreviewCacheSize
import su.afk.yummy.tv.core.preferences.settings.YaniContentLanguage
import javax.inject.Inject

internal class SettingsAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Пользователь изменил тему приложения.
     *
     * Параметры: setting, value.
     */
    fun eventAppThemeSelected(theme: AppTheme) {
        eventSettingChange(SETTING_APP_THEME, theme.name.lowercase())
    }

    /**
     * Пользователь изменил качество постеров.
     *
     * Параметры: setting, value.
     */
    fun eventPosterQualitySelected(quality: PosterQuality) {
        eventSettingChange(SETTING_POSTER_QUALITY, quality.name.lowercase())
    }

    /**
     * Пользователь изменил размер карточек постеров.
     *
     * Параметры: setting, value.
     */
    fun eventPosterCardSizeSelected(size: PosterCardSize) {
        eventSettingChange(SETTING_POSTER_CARD_SIZE, size.name.lowercase())
    }

    /**
     * Пользователь изменил предпочитаемый плеер.
     *
     * Параметры: setting, value.
     */
    fun eventPreferredPlayerSelected(player: PreferredPlayer) {
        eventSettingChange(SETTING_PREFERRED_PLAYER, player.name.lowercase())
    }

    /**
     * Пользователь включил или выключил автопереход к следующей серии.
     *
     * Параметры: setting, value.
     */
    fun eventWatchNextToggled(enabled: Boolean) {
        eventSettingChange(SETTING_WATCH_NEXT_ENABLED, enabled.toString())
    }

    /**
     * Пользователь изменил размер кэша превью.
     *
     * Параметры: setting, value.
     */
    fun eventPreviewCacheSizeSelected(size: PreviewCacheSize) {
        eventSettingChange(SETTING_PREVIEW_CACHE_SIZE, size.name.lowercase())
    }

    /**
     * Пользователь включил или выключил автопропуск опенингов и эндингов.
     *
     * Параметры: setting, value.
     */
    fun eventAutoSkipOpeningsEndingsToggled(enabled: Boolean) {
        eventSettingChange(SETTING_AUTO_SKIP_OPENINGS_ENDINGS, enabled.toString())
    }

    /**
     * Пользователь изменил язык контента Yani.
     *
     * Параметры: setting, value.
     */
    fun eventContentLanguageSelected(language: YaniContentLanguage) {
        eventSettingChange(SETTING_CONTENT_LANGUAGE, language.name.lowercase())
    }

    /**
     * Пользователь изменил порядок кнопок на экране деталей.
     *
     * Параметры: setting, value.
     */
    fun eventDetailsButtonMoved(
        action: DetailsButtonAction,
        direction: DetailsButtonMoveDirection
    ) {
        eventSettingChange(
            setting = SETTING_DETAILS_BUTTON_ORDER,
            value = "${action.name.lowercase()}_${direction.name.lowercase()}",
        )
    }

    /**
     * Пользователь сбросил порядок кнопок на экране деталей.
     *
     * Параметры: setting, value.
     */
    fun eventDetailsButtonOrderReset() {
        eventSettingChange(SETTING_DETAILS_BUTTON_ORDER, VALUE_RESET)
    }

    private fun eventSettingChange(setting: String, value: String) {
        tracker.track(
            EVENT_SETTING_CHANGE,
            analyticsParamsOf(
                PARAM_SETTING to setting,
                PARAM_VALUE to value,
            ),
        )
    }

    /**
     * Пользователь запросил отображение preview channel на Android TV.
     */
    fun eventRequestPreviewChannelBrowsable() {
        tracker.track(EVENT_REQUEST_PREVIEW_CHANNEL_BROWSABLE)
    }

    /**
     * Пользователь открыл настройку порядка кнопок на экране деталей.
     */
    fun eventDetailsButtonOrderSelected() {
        tracker.track(EVENT_DETAILS_BUTTON_ORDER_SELECTED)
    }

    internal companion object {
        private const val PARAM_SETTING = "setting"
        private const val PARAM_VALUE = "value"
        private const val SETTING_APP_THEME = "app_theme"
        private const val SETTING_AUTO_SKIP_OPENINGS_ENDINGS = "auto_skip_openings_endings"
        private const val SETTING_CONTENT_LANGUAGE = "content_language"
        private const val SETTING_DETAILS_BUTTON_ORDER = "details_button_order"
        private const val SETTING_POSTER_CARD_SIZE = "poster_card_size"
        private const val SETTING_POSTER_QUALITY = "poster_quality"
        private const val SETTING_PREFERRED_PLAYER = "preferred_player"
        private const val SETTING_PREVIEW_CACHE_SIZE = "preview_cache_size"
        private const val SETTING_WATCH_NEXT_ENABLED = "watch_next_enabled"
        private const val VALUE_RESET = "reset"

        const val EVENT_SETTING_CHANGE = "setting_change"
        const val EVENT_REQUEST_PREVIEW_CHANNEL_BROWSABLE =
            "settings_request_preview_channel_browsable"
        const val EVENT_DETAILS_BUTTON_ORDER_SELECTED = "settings_details_button_order_selected"
    }
}
