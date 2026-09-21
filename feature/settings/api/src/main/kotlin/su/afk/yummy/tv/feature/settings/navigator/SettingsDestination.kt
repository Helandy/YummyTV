package su.afk.yummy.tv.feature.settings.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SettingsDestination : NavKey

@Serializable
data object SettingsDetailsButtonOrderDestination : NavKey

/** Категории мобильных настроек: у каждой свой экран, чтобы не листать один длинный список. */
@Serializable
enum class SettingsCategory {
    GENERAL,
    APPEARANCE,
    PLAYER,
    PLAYBACK,
    WATCH_PROGRESS,
    SUBTITLES,
    STORAGE,
    API,
}

@Serializable
data class SettingsCategoryDestination(val category: SettingsCategory) : NavKey
