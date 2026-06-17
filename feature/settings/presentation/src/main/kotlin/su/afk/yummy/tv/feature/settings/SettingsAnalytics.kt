package su.afk.yummy.tv.feature.settings

import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.core.preferences.settings.AppTheme
import su.afk.yummy.tv.core.preferences.settings.DetailsButtonAction
import su.afk.yummy.tv.core.preferences.settings.LibraryContinueWatchingCardSize
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
     * Пользователь открыл экран настроек.
     */
    fun eventScreenOpened() {
        tracker.track(EVENT_SCREEN_OPENED)
    }

    /**
     * Пользователь открыл экран настройки порядка кнопок деталей.
     */
    fun eventDetailsButtonOrderScreenOpened() {
        tracker.track(EVENT_DETAILS_BUTTON_ORDER_SCREEN_OPENED)
    }

    /**
     * Пользователь изменил тему приложения.
     *
     * Параметры: value.
     */
    fun eventAppThemeSelected(theme: AppTheme) {
        tracker.track(
            EVENT_APP_THEME_SELECTED,
            analyticsParamsOf(PARAM_VALUE to theme.name.lowercase()),
        )
    }

    /**
     * Пользователь изменил качество постеров.
     *
     * Параметры: value.
     */
    fun eventPosterQualitySelected(quality: PosterQuality) {
        tracker.track(
            EVENT_POSTER_QUALITY_SELECTED,
            analyticsParamsOf(PARAM_VALUE to quality.name.lowercase()),
        )
    }

    /**
     * Пользователь изменил размер карточек постеров.
     *
     * Параметры: value.
     */
    fun eventPosterCardSizeSelected(size: PosterCardSize) {
        tracker.track(
            EVENT_POSTER_CARD_SIZE_SELECTED,
            analyticsParamsOf(PARAM_VALUE to size.name.lowercase()),
        )
    }

    /**
     * Пользователь изменил размер карточек продолжения просмотра в библиотеке.
     *
     * Параметры: value.
     */
    fun eventLibraryContinueWatchingCardSizeSelected(size: LibraryContinueWatchingCardSize) {
        tracker.track(
            EVENT_LIBRARY_CONTINUE_WATCHING_CARD_SIZE_SELECTED,
            analyticsParamsOf(PARAM_VALUE to size.name.lowercase()),
        )
    }

    /**
     * Пользователь изменил предпочитаемый плеер.
     *
     * Параметры: value.
     */
    fun eventPreferredPlayerSelected(player: PreferredPlayer) {
        tracker.track(
            EVENT_PREFERRED_PLAYER_SELECTED,
            analyticsParamsOf(PARAM_VALUE to player.name.lowercase()),
        )
    }

    /**
     * Пользователь включил или выключил автопереход к следующей серии.
     *
     * Параметры: target_state.
     */
    fun eventWatchNextToggled(enabled: Boolean) {
        tracker.track(
            EVENT_WATCH_NEXT_TOGGLED,
            analyticsParamsOf(PARAM_TARGET_STATE to enabled),
        )
    }

    /**
     * Пользователь изменил размер кэша превью.
     *
     * Параметры: value.
     */
    fun eventPreviewCacheSizeSelected(size: PreviewCacheSize) {
        tracker.track(
            EVENT_PREVIEW_CACHE_SIZE_SELECTED,
            analyticsParamsOf(PARAM_VALUE to size.name.lowercase()),
        )
    }

    /**
     * Пользователь включил или выключил автопропуск опенингов и эндингов.
     *
     * Параметры: target_state.
     */
    fun eventAutoSkipOpeningsEndingsToggled(enabled: Boolean) {
        tracker.track(
            EVENT_AUTO_SKIP_OPENINGS_ENDINGS_TOGGLED,
            analyticsParamsOf(PARAM_TARGET_STATE to enabled),
        )
    }

    /**
     * Пользователь изменил язык контента Yani.
     *
     * Параметры: value.
     */
    fun eventContentLanguageSelected(language: YaniContentLanguage) {
        tracker.track(
            EVENT_CONTENT_LANGUAGE_SELECTED,
            analyticsParamsOf(PARAM_VALUE to language.name.lowercase()),
        )
    }

    /**
     * Пользователь изменил порядок кнопок на экране деталей.
     *
     * Параметры: action, direction.
     */
    fun eventDetailsButtonMoved(
        action: DetailsButtonAction,
        direction: DetailsButtonMoveDirection
    ) {
        tracker.track(
            EVENT_DETAILS_BUTTON_ORDER_MOVED,
            analyticsParamsOf(
                PARAM_ACTION to action.name.lowercase(),
                PARAM_DIRECTION to direction.name.lowercase(),
            ),
        )
    }

    /**
     * Пользователь сбросил порядок кнопок на экране деталей.
     */
    fun eventDetailsButtonOrderReset() {
        tracker.track(EVENT_DETAILS_BUTTON_ORDER_RESET)
    }

    /**
     * Пользователь запросил отображение preview channel на Android TV.
     */
    fun eventRequestPreviewChannelBrowsable() {
        tracker.track(EVENT_REQUEST_PREVIEW_CHANNEL_BROWSABLE)
    }

    internal companion object {
        private const val PARAM_ACTION = "action"
        private const val PARAM_DIRECTION = "direction"
        private const val PARAM_TARGET_STATE = "target_state"
        private const val PARAM_VALUE = "value"

        const val EVENT_APP_THEME_SELECTED = "settings_app_theme_selected"
        const val EVENT_AUTO_SKIP_OPENINGS_ENDINGS_TOGGLED =
            "settings_auto_skip_openings_endings_toggled"
        const val EVENT_CONTENT_LANGUAGE_SELECTED = "settings_content_language_selected"
        const val EVENT_DETAILS_BUTTON_ORDER_MOVED = "settings_details_button_order_moved"
        const val EVENT_DETAILS_BUTTON_ORDER_RESET = "settings_details_button_order_reset"
        const val EVENT_SCREEN_OPENED = "settings_screen"
        const val EVENT_DETAILS_BUTTON_ORDER_SCREEN_OPENED =
            "settings_details_button_order_screen"
        const val EVENT_LIBRARY_CONTINUE_WATCHING_CARD_SIZE_SELECTED =
            "settings_library_continue_watching_card_size_selected"
        const val EVENT_POSTER_CARD_SIZE_SELECTED = "settings_poster_card_size_selected"
        const val EVENT_POSTER_QUALITY_SELECTED = "settings_poster_quality_selected"
        const val EVENT_PREFERRED_PLAYER_SELECTED = "settings_preferred_player_selected"
        const val EVENT_PREVIEW_CACHE_SIZE_SELECTED = "settings_preview_cache_size_selected"
        const val EVENT_REQUEST_PREVIEW_CHANNEL_BROWSABLE =
            "settings_request_preview_channel_browsable"
        const val EVENT_WATCH_NEXT_TOGGLED = "settings_watch_next_toggled"
    }
}
