package su.afk.yummy.tv.core.preferences.settings.datastore

import androidx.datastore.preferences.core.Preferences
import su.afk.yummy.tv.core.model.settings.VideoEnhancementMode
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.deblurEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.videoEnhancementModeKey

/**
 * Режим улучшения видео. Пока его не выбирали, переносится из старого флага Deblur: включённый Deblur
 * становится [VideoEnhancementMode.SHARPEN].
 */
internal fun Preferences.videoEnhancementMode(): VideoEnhancementMode {
    val legacy = if (this[deblurEnabledKey] == true) VideoEnhancementMode.SHARPEN else VideoEnhancementMode.OFF
    return enum(videoEnhancementModeKey, legacy)
}
