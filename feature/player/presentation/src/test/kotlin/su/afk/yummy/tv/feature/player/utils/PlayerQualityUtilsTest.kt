package su.afk.yummy.tv.feature.player.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.testing.BaseUnitTest

class PlayerQualityUtilsTest : BaseUnitTest() {

    private val labels = listOf("360p", "480p", "720p", "1080p")

    @Test
    fun `limit label is the highest quality not above the ceiling`() {
        assertEquals("720p", adaptiveLimitLabel(labels, 720))
        assertEquals("720p", adaptiveLimitLabel(labels, 900))
    }

    @Test
    fun `limit label is the best available when the ceiling is above every quality`() {
        assertEquals("1080p", adaptiveLimitLabel(labels, 2160))
    }

    @Test
    fun `limit label falls back to the best available when every quality is above the ceiling`() {
        assertEquals("1080p", adaptiveLimitLabel(listOf("720p", "1080p"), 480))
    }

    @Test
    fun `limit label is null without numeric qualities`() {
        assertNull(adaptiveLimitLabel(listOf("auto"), 720))
    }

    @Test
    fun `preferred quality matches the label height`() {
        assertEquals(PreferredVideoQuality.P480, preferredQualityForLabel("480p"))
        assertEquals(PreferredVideoQuality.P1080, preferredQualityForLabel("1080p"))
    }

    @Test
    fun `preferred quality rounds a nonstandard height down`() {
        assertEquals(PreferredVideoQuality.P720, preferredQualityForLabel("800p"))
        assertEquals(PreferredVideoQuality.P360, preferredQualityForLabel("240p"))
        assertNull(preferredQualityForLabel("auto"))
    }
}
