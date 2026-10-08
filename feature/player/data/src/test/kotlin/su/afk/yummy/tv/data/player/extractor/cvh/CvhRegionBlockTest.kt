package su.afk.yummy.tv.data.player.extractor.cvh

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class CvhRegionBlockTest : BaseUnitTest() {

    @Test
    fun `empty playlist with the blocked tag is a region block`() {
        assertTrue(isCvhRegionBlocked(tags = listOf(5), hasItems = false))
    }

    @Test
    fun `blocked tag among other tags still counts`() {
        assertTrue(isCvhRegionBlocked(tags = listOf(1, 3, 5), hasItems = false))
    }

    @Test
    fun `blocked tag with playable items is not a region block`() {
        assertFalse(isCvhRegionBlocked(tags = listOf(5), hasItems = true))
    }

    @Test
    fun `empty playlist without the blocked tag is not a region block`() {
        assertFalse(isCvhRegionBlocked(tags = emptyList(), hasItems = false))
        assertFalse(isCvhRegionBlocked(tags = listOf(1, 3), hasItems = false))
    }
}
