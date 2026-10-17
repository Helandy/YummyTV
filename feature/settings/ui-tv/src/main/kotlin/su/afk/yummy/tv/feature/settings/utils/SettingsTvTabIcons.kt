package su.afk.yummy.tv.feature.settings.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.feature.settings.model.SettingsTab

/** Иконка категории в левом списке ТВ-настроек; те же, что у категорий на мобилке. */
internal val SettingsTab.icon: ImageVector
    get() = when (this) {
        SettingsTab.GENERAL -> Icons.Outlined.Tune
        SettingsTab.APPEARANCE -> Icons.Outlined.Palette
        SettingsTab.PLAYER -> Icons.Outlined.PlayCircle
        SettingsTab.PLAYBACK -> Icons.Outlined.SkipNext
        SettingsTab.WATCH_PROGRESS -> Icons.Outlined.History
        SettingsTab.SUBTITLES -> Icons.Outlined.Subtitles
        SettingsTab.STORAGE -> Icons.Outlined.Storage
        SettingsTab.API -> Icons.Outlined.Key
        SettingsTab.ABOUT -> Icons.Outlined.Info
    }
