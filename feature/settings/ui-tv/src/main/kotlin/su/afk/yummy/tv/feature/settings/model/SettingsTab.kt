package su.afk.yummy.tv.feature.settings.model

import androidx.annotation.StringRes
import su.afk.yummy.tv.feature.settings.R

internal enum class SettingsTab(@param:StringRes val labelRes: Int) {
    THEME(R.string.settings_tab_theme),
    POSTERS(R.string.settings_tab_posters),
    DETAILS(R.string.settings_tab_details),
    PLAYER(R.string.settings_tab_player),
    CACHE(R.string.settings_tab_cache),
    API(R.string.settings_tab_api),
    TV_HOME(R.string.settings_tab_tv_home),
    ABOUT(R.string.settings_tab_about),
}
