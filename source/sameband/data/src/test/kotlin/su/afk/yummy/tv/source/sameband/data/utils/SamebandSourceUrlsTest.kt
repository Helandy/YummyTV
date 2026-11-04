package su.afk.yummy.tv.source.sameband.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.source.sameband.domain.model.SamebandTarget

class SamebandSourceUrlsTest {

    @Test
    fun `target round trips through the url`() {
        val target = SamebandTarget("1234-grand-blue-3", 7)

        assertEquals("source://sameband/1234-grand-blue-3/7", target.toSourceUrl())
        assertEquals(target, parseSamebandSourceUrl(target.toSourceUrl()))
    }

    @Test
    fun `malformed urls are rejected`() {
        assertNull(parseSamebandSourceUrl("source://sameband/1234-x"))
        assertNull(parseSamebandSourceUrl("source://sameband/1234-x/0"))
        assertNull(parseSamebandSourceUrl("source://sameband/1234-x/x"))
        assertNull(parseSamebandSourceUrl("source://sameband/blue/1"))
        assertNull(parseSamebandSourceUrl("source://sameband/../etc/1"))
        assertNull(parseSamebandSourceUrl("source://anidub/1234-x/1"))
    }

    @Test
    fun `only sameband urls are recognized`() {
        assertTrue(isSamebandSourceUrl("source://sameband/1-a/1"))
        assertFalse(isSamebandSourceUrl("source://anidub/1-a/1"))
    }

    @Test
    fun `slug is taken from the page url`() {
        assertEquals("1234-grand-blue-3", samebandSlugFromUrl("https://sameband.studio/anime/1234-grand-blue-3.html"))
        assertNull(samebandSlugFromUrl("https://sameband.studio/"))
    }
}
