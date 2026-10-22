package su.afk.yummy.tv.source.amedia.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTarget

class AmediaSourceUrlsTest : BaseUnitTest() {
    @Test
    fun `url round trips`() {
        val target = AmediaTarget(47997)

        assertEquals("source://amedia/47997", target.toSourceUrl())
        assertEquals(target, parseAmediaSourceUrl(target.toSourceUrl()))
    }

    @Test
    fun `prefix check ignores case and rejects other sources`() {
        assertTrue(isAmediaSourceUrl("SOURCE://Amedia/1"))
        assertFalse(isAmediaSourceUrl("source://anistar/1/1"))
    }

    @Test
    fun `malformed urls are rejected`() {
        assertNull(parseAmediaSourceUrl("source://amedia/"))
        assertNull(parseAmediaSourceUrl("source://amedia/x"))
        assertNull(parseAmediaSourceUrl("source://amedia/0"))
        assertNull(parseAmediaSourceUrl("source://amedia/1/2"))
        assertNull(parseAmediaSourceUrl("https://aser.pro/vod/1"))
    }
}
