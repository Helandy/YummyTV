package su.afk.yummy.tv.feature.settings.model

import androidx.annotation.StringRes
import su.afk.yummy.tv.feature.settings.R

/** Категории ТВ-настроек в левом списке; совпадают с мобильными плюс «О приложении». */
internal enum class SettingsTab(@param:StringRes val labelRes: Int) {
    GENERAL(R.string.settings_tv_category_general),
    APPEARANCE(R.string.settings_tv_category_appearance),
    PLAYER(R.string.settings_tv_category_player),
    PLAYBACK(R.string.settings_tv_category_playback),
    WATCH_PROGRESS(R.string.settings_tv_category_watch_progress),
    SUBTITLES(R.string.settings_tv_category_subtitles),
    STORAGE(R.string.settings_tv_category_storage),
    API(R.string.settings_tv_category_api),
    ABOUT(R.string.settings_tv_category_about),
}
