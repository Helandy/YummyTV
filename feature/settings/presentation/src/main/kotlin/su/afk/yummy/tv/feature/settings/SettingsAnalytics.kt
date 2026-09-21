package su.afk.yummy.tv.feature.settings

import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.utils.analyticsParamsOf
import su.afk.yummy.tv.core.model.settings.AppTheme
import su.afk.yummy.tv.core.model.settings.BackgroundStyle
import su.afk.yummy.tv.core.model.settings.DetailsButtonAction
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.PlayerBufferProfile
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.model.settings.PosterQuality
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection
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

    /** Пользователь изменил тип интерфейса приложения. */
    fun eventInterfaceModeSelected(mode: AppInterfaceMode) {
        tracker.track(
            EVENT_INTERFACE_MODE_SELECTED,
            analyticsParamsOf(PARAM_VALUE to mode.name.lowercase()),
        )
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
     * Пользователь изменил цвет фона интерфейса.
     *
     * Параметры: value.
     */
    fun eventBackgroundStyleSelected(style: BackgroundStyle) {
        tracker.track(
            EVENT_BACKGROUND_STYLE_SELECTED,
            analyticsParamsOf(PARAM_VALUE to style.name.lowercase()),
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
     * Пользователь изменил предпочитаемое качество видео.
     *
     * Параметры: value.
     */
    fun eventPreferredVideoQualitySelected(quality: PreferredVideoQuality) {
        tracker.track(
            EVENT_PREFERRED_VIDEO_QUALITY_SELECTED,
            analyticsParamsOf(PARAM_VALUE to quality.name.lowercase()),
        )
    }

    /**
     * Пользователь изменил размер буфера плеера.
     *
     * Параметры: value.
     */
    fun eventPlayerBufferProfileSelected(profile: PlayerBufferProfile) {
        tracker.track(
            EVENT_PLAYER_BUFFER_PROFILE_SELECTED,
            analyticsParamsOf(PARAM_VALUE to profile.name.lowercase()),
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
    fun eventPreviewCacheSizeSelected(size: Int) {
        tracker.track(
            EVENT_PREVIEW_CACHE_SIZE_SELECTED,
            analyticsParamsOf(PARAM_VALUE to size),
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

    /** Пользователь изменил задержку перед автопропуском опенинга/эндинга. */
    fun eventAutoSkipDelayChanged(seconds: Int) {
        tracker.track(
            EVENT_AUTO_SKIP_DELAY_CHANGED,
            analyticsParamsOf(PARAM_VALUE to seconds),
        )
    }

    /** Пользователь включил или выключил автовоспроизведение следующей серии. */
    fun eventAutoPlayNextEpisodeToggled(enabled: Boolean) {
        tracker.track(
            EVENT_AUTO_PLAY_NEXT_EPISODE_TOGGLED,
            analyticsParamsOf(PARAM_TARGET_STATE to enabled),
        )
    }

    /** Пользователь изменил задержку перед авто-переключением на следующую серию. */
    fun eventNextEpisodeSwitchDelayChanged(seconds: Int) {
        tracker.track(
            EVENT_NEXT_EPISODE_SWITCH_DELAY_CHANGED,
            analyticsParamsOf(PARAM_VALUE to seconds),
        )
    }

    /** Пользователь включил или выключил запрос выбора озвучки при нажатии "Смотреть". */
    fun eventAskDubbingOnWatchToggled(enabled: Boolean) {
        tracker.track(
            EVENT_ASK_DUBBING_ON_WATCH_TOGGLED,
            analyticsParamsOf(PARAM_TARGET_STATE to enabled),
        )
    }

    /** Пользователь включил или выключил плавающий режим мобильного плеера. */
    fun eventPictureInPictureToggled(enabled: Boolean) {
        tracker.track(
            EVENT_PICTURE_IN_PICTURE_TOGGLED,
            analyticsParamsOf(PARAM_TARGET_STATE to enabled),
        )
    }

    /** Пользователь изменил режим принудительной ориентации плеера. */
    fun eventPlayerOrientationModeSelected(mode: PlayerOrientationMode) {
        tracker.track(
            EVENT_PLAYER_ORIENTATION_MODE_SELECTED,
            analyticsParamsOf(PARAM_VALUE to mode.name.lowercase()),
        )
    }

    /** Пользователь включил или выключил автовыгрузку серии после скачивания. */
    fun eventVideoExportAutoToggled(enabled: Boolean) {
        tracker.track(
            EVENT_VIDEO_EXPORT_AUTO_TOGGLED,
            analyticsParamsOf(PARAM_TARGET_STATE to enabled),
        )
    }

    /** Пользователь включил или выключил перехват кнопок громкости в ТВ-плеере. */
    fun eventTvPlayerVolumeKeysToggled(enabled: Boolean) {
        tracker.track(
            EVENT_TV_PLAYER_VOLUME_KEYS_TOGGLED,
            analyticsParamsOf(PARAM_TARGET_STATE to enabled),
        )
    }

    /** Пользователь включил или выключил продвинутую (внутреннюю) громкость плеера. */
    fun eventAdvancedPlayerVolumeToggled(enabled: Boolean) {
        tracker.track(
            EVENT_ADVANCED_PLAYER_VOLUME_TOGGLED,
            analyticsParamsOf(PARAM_TARGET_STATE to enabled),
        )
    }

    /** Пользователь включил повторный показ обучения жестам мобильного плеера. */
    fun eventMobilePlayerGestureTutorialReset() {
        tracker.track(EVENT_MOBILE_PLAYER_GESTURE_TUTORIAL_RESET)
    }

    /** Пользователь включил повторный показ обучения управлению ТВ-плеером. */
    fun eventTvPlayerControlsTutorialReset() {
        tracker.track(EVENT_TV_PLAYER_CONTROLS_TUTORIAL_RESET)
    }

    /** Пользователь включил или выключил отображение года у тайтлов в топе. */
    fun eventShowTopTitleYearToggled(enabled: Boolean) {
        tracker.track(
            EVENT_SHOW_TOP_TITLE_YEAR_TOGGLED,
            analyticsParamsOf(PARAM_TARGET_STATE to enabled),
        )
    }

    /** Пользователь включил или выключил отображение года у тайтлов в библиотеке. */
    fun eventShowLibraryTitleYearToggled(enabled: Boolean) {
        tracker.track(
            EVENT_SHOW_LIBRARY_TITLE_YEAR_TOGGLED,
            analyticsParamsOf(PARAM_TARGET_STATE to enabled),
        )
    }

    /**
     * Пользователь включил или выключил предложение следующей серии после завершения текущей.
     *
     * Параметры: target_state.
     */
    fun eventSuggestNextEpisodeOnWatchedToggled(enabled: Boolean) {
        tracker.track(
            EVENT_SUGGEST_NEXT_EPISODE_ON_WATCHED_TOGGLED,
            analyticsParamsOf(PARAM_TARGET_STATE to enabled),
        )
    }

    /**
     * Пользователь включил или выключил запрос последнего прогресса при продолжении просмотра.
     *
     * Параметры: target_state.
     */
    fun eventRefreshContinueWatchingProgressOnLaunchToggled(enabled: Boolean) {
        tracker.track(
            EVENT_REFRESH_CONTINUE_WATCHING_PROGRESS_ON_LAUNCH_TOGGLED,
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

    /** Пользователь включил или выключил сохранение последнего поиска. */
    fun eventSaveLastSearchToggled(enabled: Boolean) {
        tracker.track(
            EVENT_SAVE_LAST_SEARCH_TOGGLED,
            analyticsParamsOf(PARAM_TARGET_STATE to enabled),
        )
    }

    internal companion object {
        private const val PARAM_ACTION = "action"
        private const val PARAM_DIRECTION = "direction"
        private const val PARAM_TARGET_STATE = "target_state"
        private const val PARAM_VALUE = "value"

        const val EVENT_APP_THEME_SELECTED = "settings_app_theme_selected"
        const val EVENT_BACKGROUND_STYLE_SELECTED = "settings_background_style_selected"
        const val EVENT_AUTO_SKIP_OPENINGS_ENDINGS_TOGGLED =
            "settings_auto_skip_openings_endings_toggled"
        const val EVENT_AUTO_SKIP_DELAY_CHANGED = "settings_auto_skip_delay_changed"
        const val EVENT_CONTENT_LANGUAGE_SELECTED = "settings_content_language_selected"
        const val EVENT_DETAILS_BUTTON_ORDER_MOVED = "settings_details_button_order_moved"
        const val EVENT_DETAILS_BUTTON_ORDER_RESET = "settings_details_button_order_reset"
        const val EVENT_INTERFACE_MODE_SELECTED = "settings_interface_mode_selected"
        const val EVENT_SCREEN_OPENED = "settings_screen"
        const val EVENT_SHOW_TOP_TITLE_YEAR_TOGGLED =
            "settings_show_top_title_year_toggled"
        const val EVENT_SHOW_LIBRARY_TITLE_YEAR_TOGGLED =
            "settings_show_library_title_year_toggled"
        const val EVENT_DETAILS_BUTTON_ORDER_SCREEN_OPENED =
            "settings_details_button_order_screen"
        const val EVENT_LIBRARY_CONTINUE_WATCHING_CARD_SIZE_SELECTED =
            "settings_library_continue_watching_card_size_selected"
        const val EVENT_POSTER_CARD_SIZE_SELECTED = "settings_poster_card_size_selected"
        const val EVENT_POSTER_QUALITY_SELECTED = "settings_poster_quality_selected"
        const val EVENT_PREFERRED_PLAYER_SELECTED = "settings_preferred_player_selected"
        const val EVENT_PLAYER_BUFFER_PROFILE_SELECTED = "settings_player_buffer_profile_selected"
        const val EVENT_PREFERRED_VIDEO_QUALITY_SELECTED =
            "settings_preferred_video_quality_selected"
        const val EVENT_PREVIEW_CACHE_SIZE_SELECTED = "settings_preview_cache_size_selected"
        const val EVENT_REQUEST_PREVIEW_CHANNEL_BROWSABLE =
            "settings_request_preview_channel_browsable"
        const val EVENT_REFRESH_CONTINUE_WATCHING_PROGRESS_ON_LAUNCH_TOGGLED =
            "settings_refresh_continue_watching_progress_on_launch_toggled"
        const val EVENT_SUGGEST_NEXT_EPISODE_ON_WATCHED_TOGGLED =
            "settings_suggest_next_episode_on_watched_toggled"
        const val EVENT_AUTO_PLAY_NEXT_EPISODE_TOGGLED =
            "settings_auto_play_next_episode_toggled"
        const val EVENT_NEXT_EPISODE_SWITCH_DELAY_CHANGED =
            "settings_next_episode_switch_delay_changed"
        const val EVENT_ASK_DUBBING_ON_WATCH_TOGGLED =
            "settings_ask_dubbing_on_watch_toggled"
        const val EVENT_PICTURE_IN_PICTURE_TOGGLED =
            "settings_picture_in_picture_toggled"
        const val EVENT_PLAYER_ORIENTATION_MODE_SELECTED =
            "settings_player_orientation_mode_selected"
        const val EVENT_VIDEO_EXPORT_AUTO_TOGGLED = "settings_video_export_auto_toggled"
        const val EVENT_TV_PLAYER_VOLUME_KEYS_TOGGLED =
            "settings_tv_player_volume_keys_toggled"
        const val EVENT_ADVANCED_PLAYER_VOLUME_TOGGLED =
            "settings_advanced_player_volume_toggled"
        const val EVENT_MOBILE_PLAYER_GESTURE_TUTORIAL_RESET =
            "settings_mobile_player_gesture_tutorial_reset"
        const val EVENT_TV_PLAYER_CONTROLS_TUTORIAL_RESET =
            "settings_tv_player_controls_tutorial_reset"
        const val EVENT_WATCH_NEXT_TOGGLED = "settings_watch_next_toggled"
        const val EVENT_SAVE_LAST_SEARCH_TOGGLED = "settings_save_last_search_toggled"
    }
}
