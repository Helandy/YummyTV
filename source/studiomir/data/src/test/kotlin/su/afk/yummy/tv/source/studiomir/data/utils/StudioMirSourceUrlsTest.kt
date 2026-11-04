package su.afk.yummy.tv.source.studiomir.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.source.studiomir.domain.model.StudioMirTarget

class StudioMirSourceUrlsTest {

    @Test
    fun `target round trips through the url`() {
        val target = StudioMirTarget("12254-blich-tysjacheletnjaja-krovavaja-vojna-zakljuchitelnaja-chast", 7)

        assertEquals("source://studiomir/12254-blich-tysjacheletnjaja-krovavaja-vojna-zakljuchitelnaja-chast/7", target.toSourceUrl())
        assertEquals(target, parseStudioMirSourceUrl(target.toSourceUrl()))
    }

    @Test
    fun `malformed urls are rejected`() {
        assertNull(parseStudioMirSourceUrl("source://studiomir/12254-blich"))
        assertNull(parseStudioMirSourceUrl("source://studiomir/12254-blich/0"))
        assertNull(parseStudioMirSourceUrl("source://studiomir/12254-blich/x"))
        assertNull(parseStudioMirSourceUrl("source://studiomir/blich/1"))
        assertNull(parseStudioMirSourceUrl("source://studiomir/../etc/1"))
        assertNull(parseStudioMirSourceUrl("source://aniboom/0BzM07Pq71J/1/16"))
    }

    @Test
    fun `only studiomir urls are recognised`() {
        assertTrue(isStudioMirSourceUrl("source://studiomir/12254-blich/1"))
        assertFalse(isStudioMirSourceUrl("source://aniboom/x/1/1"))
    }
}
