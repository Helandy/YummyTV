package su.afk.yummy.tv.feature.settings.model

import androidx.annotation.StringRes
import su.afk.yummy.tv.feature.settings.R

/** Пункты ТВ-настроек с выбором из вариантов: по OK варианты открываются в третьей колонке. */
internal enum class SettingsTvPicker(@param:StringRes val titleRes: Int) {
    INTERFACE_MODE(R.string.settings_tab_interface),
    CONTENT_LANGUAGE(R.string.settings_tab_language),
    THEME(R.string.settings_tab_theme),
    BACKGROUND(R.string.settings_tab_background),
    POSTER_SIZE(R.string.settings_poster_size_title),
    CONTINUE_WATCHING_SIZE(R.string.settings_library_continue_watching_card_size_title),
    POSTER_QUALITY(R.string.settings_poster_quality_title),
    DETAILS_BUTTON_ORDER(R.string.settings_tv_details_buttons_order),
    PREFERRED_PLAYER(R.string.settings_tab_player_source),
    SUBTITLE_COLOR(R.string.settings_subtitle_color_title),
    SUBTITLE_BACKGROUND(R.string.settings_subtitle_background_title),
}
