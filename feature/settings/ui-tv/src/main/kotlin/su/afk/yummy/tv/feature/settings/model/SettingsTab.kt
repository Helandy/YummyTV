package su.afk.yummy.tv.feature.settings.model

import androidx.annotation.StringRes
import su.afk.yummy.tv.feature.settings.R

internal enum class SettingsTab(@param:StringRes val labelRes: Int) {
    THEME(R.string.settings_tab_theme),
    PLAYER(R.string.settings_tab_player),
    POSTER_SIZE(R.string.settings_tab_poster_size),
    POSTERS(R.string.settings_tab_poster_quality),
    CONTINUE_WATCHING(R.string.settings_tab_continue_watching),
    DETAILS(R.string.settings_tab_details),
    CACHE(R.string.settings_tab_cache),
    LANGUAGE(R.string.settings_tab_language),
    API(R.string.settings_tab_api),
    TV_HOME(R.string.settings_tab_tv_home),
    ABOUT(R.string.settings_tab_about),
}
