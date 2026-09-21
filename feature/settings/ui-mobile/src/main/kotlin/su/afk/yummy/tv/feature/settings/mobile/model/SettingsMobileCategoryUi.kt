package su.afk.yummy.tv.feature.settings.mobile.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.feature.settings.mobile.R
import su.afk.yummy.tv.feature.settings.navigator.SettingsCategory

@get:StringRes
internal val SettingsCategory.titleRes: Int
    get() = when (this) {
        SettingsCategory.GENERAL -> R.string.settings_mobile_category_general
        SettingsCategory.APPEARANCE -> R.string.settings_mobile_category_appearance
        SettingsCategory.PLAYER -> R.string.settings_mobile_category_player
        SettingsCategory.PLAYBACK -> R.string.settings_mobile_category_playback
        SettingsCategory.WATCH_PROGRESS -> R.string.settings_mobile_category_watch_progress
        SettingsCategory.SUBTITLES -> R.string.settings_mobile_category_subtitles
        SettingsCategory.STORAGE -> R.string.settings_mobile_category_storage
        SettingsCategory.API -> R.string.settings_mobile_category_api
    }

@get:StringRes
internal val SettingsCategory.hintRes: Int
    get() = when (this) {
        SettingsCategory.GENERAL -> R.string.settings_mobile_category_general_hint
        SettingsCategory.APPEARANCE -> R.string.settings_mobile_category_appearance_hint
        SettingsCategory.PLAYER -> R.string.settings_mobile_category_player_hint
        SettingsCategory.PLAYBACK -> R.string.settings_mobile_category_playback_hint
        SettingsCategory.WATCH_PROGRESS -> R.string.settings_mobile_category_watch_progress_hint
        SettingsCategory.SUBTITLES -> R.string.settings_mobile_category_subtitles_hint
        SettingsCategory.STORAGE -> R.string.settings_mobile_category_storage_hint
        SettingsCategory.API -> R.string.settings_mobile_category_api_hint
    }

internal val SettingsCategory.icon: ImageVector
    get() = when (this) {
        SettingsCategory.GENERAL -> Icons.Outlined.Tune
        SettingsCategory.APPEARANCE -> Icons.Outlined.Palette
        SettingsCategory.PLAYER -> Icons.Outlined.PlayCircle
        SettingsCategory.PLAYBACK -> Icons.Outlined.SkipNext
        SettingsCategory.WATCH_PROGRESS -> Icons.Outlined.History
        SettingsCategory.SUBTITLES -> Icons.Outlined.Subtitles
        SettingsCategory.STORAGE -> Icons.Outlined.Storage
        SettingsCategory.API -> Icons.Outlined.Key
    }
