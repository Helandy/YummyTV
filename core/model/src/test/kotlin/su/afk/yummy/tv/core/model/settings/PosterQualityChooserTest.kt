package su.afk.yummy.tv.core.model.settings

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class PosterQualityChooserTest : BaseUnitTest() {

    @Test
    fun `avif support gives mega`() {
        assertEquals(PosterQuality.MEGA, choosePosterQuality(avifSupported = true, webpSupported = true))
        assertEquals(PosterQuality.MEGA, choosePosterQuality(avifSupported = true, webpSupported = false))
    }

    @Test
    fun `webp only gives standard`() {
        assertEquals(PosterQuality.STANDARD, choosePosterQuality(avifSupported = false, webpSupported = true))
    }

    @Test
    fun `no modern format falls back to jpeg`() {
        assertEquals(PosterQuality.HIGH, choosePosterQuality(avifSupported = false, webpSupported = false))
    }
}
