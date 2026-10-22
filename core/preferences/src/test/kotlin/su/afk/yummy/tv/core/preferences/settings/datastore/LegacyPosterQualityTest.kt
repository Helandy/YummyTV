package su.afk.yummy.tv.core.preferences.settings.datastore

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.PosterQuality
import su.afk.yummy.tv.core.testing.BaseUnitTest

class LegacyPosterQualityTest : BaseUnitTest() {

    @Test
    fun `android 12 and newer keeps mega`() {
        assertEquals(PosterQuality.MEGA, legacyPosterQuality(sdkInt = 31))
        assertEquals(PosterQuality.MEGA, legacyPosterQuality(sdkInt = 35))
    }

    @Test
    fun `older android keeps standard`() {
        assertEquals(PosterQuality.STANDARD, legacyPosterQuality(sdkInt = 30))
        assertEquals(PosterQuality.STANDARD, legacyPosterQuality(sdkInt = 24))
    }
}
