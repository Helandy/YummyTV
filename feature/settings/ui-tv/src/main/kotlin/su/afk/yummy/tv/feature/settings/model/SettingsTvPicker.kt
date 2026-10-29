package su.afk.yummy.tv.feature.settings.model

import androidx.annotation.StringRes
import su.afk.yummy.tv.feature.settings.presentation.R

/** Пункты ТВ-настроек с выбором из вариантов: по OK варианты открываются в третьей колонке. */
internal enum class SettingsTvPicker(@param:StringRes val titleRes: Int) {
    INTERFACE_MODE(R.string.settings_tab_interface),
    CONTENT_LANGUAGE(R.string.settings_content_language_title),
    THEME(R.string.settings_tab_theme),
    BACKGROUND(R.string.settings_tab_background),
    NAV_TRANSITION(R.string.settings_nav_transition_title),
    POSTER_QUALITY(R.string.settings_poster_quality_title),
    NEW_EPISODES_SOURCES(R.string.settings_new_episodes_sources_title),
    LIBRARY_TAB_ORDER(R.string.settings_library_tabs_order),
    SUBTITLE_COLOR(R.string.settings_subtitle_color_title),
    SUBTITLE_BACKGROUND(R.string.settings_tab_background),
    SUBTITLE_EDGE(R.string.settings_subtitle_edge_title),
}
