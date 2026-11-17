package su.afk.yummy.tv.core.preferences.settings.datastore

import androidx.datastore.preferences.core.preferencesOf
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.VideoEnhancementMode
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.deblurEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.videoEnhancementModeKey
import su.afk.yummy.tv.core.testing.BaseUnitTest

/** Режим улучшения видео и перенос старого флага Deblur. */
class PreferencesVideoEnhancementTest : BaseUnitTest() {

    @Test
    fun `nothing stored means off`() {
        assertEquals(VideoEnhancementMode.OFF, preferencesOf().videoEnhancementMode())
    }

    @Test
    fun `enabled legacy deblur becomes sharpen`() {
        assertEquals(VideoEnhancementMode.SHARPEN, preferencesOf(deblurEnabledKey to true).videoEnhancementMode())
    }

    @Test
    fun `disabled legacy deblur stays off`() {
        assertEquals(VideoEnhancementMode.OFF, preferencesOf(deblurEnabledKey to false).videoEnhancementMode())
    }

    @Test
    fun `stored mode wins over the legacy flag`() {
        val prefs = preferencesOf(deblurEnabledKey to true, videoEnhancementModeKey to "QUALITY")

        assertEquals(VideoEnhancementMode.QUALITY, prefs.videoEnhancementMode())
    }

    @Test
    fun `unknown stored mode falls back to the legacy flag`() {
        val prefs = preferencesOf(deblurEnabledKey to true, videoEnhancementModeKey to "ULTRA")

        assertEquals(VideoEnhancementMode.SHARPEN, prefs.videoEnhancementMode())
    }
}
