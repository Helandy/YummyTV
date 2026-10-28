package su.afk.yummy.tv.source.anidub.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.source.anidub.domain.model.AniDubTarget

class AniDubSourceUrlsTest {

    @Test
    fun `target round trips through the url`() {
        val target = AniDubTarget("12254-blich-tysjacheletnjaja-krovavaja-vojna-zakljuchitelnaja-chast", 7)

        assertEquals("source://anidub/12254-blich-tysjacheletnjaja-krovavaja-vojna-zakljuchitelnaja-chast/7", target.toSourceUrl())
        assertEquals(target, parseAniDubSourceUrl(target.toSourceUrl()))
    }

    @Test
    fun `malformed urls are rejected`() {
        assertNull(parseAniDubSourceUrl("source://anidub/12254-blich"))
        assertNull(parseAniDubSourceUrl("source://anidub/12254-blich/0"))
        assertNull(parseAniDubSourceUrl("source://anidub/12254-blich/x"))
        assertNull(parseAniDubSourceUrl("source://anidub/blich/1"))
        assertNull(parseAniDubSourceUrl("source://anidub/../etc/1"))
        assertNull(parseAniDubSourceUrl("source://aniboom/0BzM07Pq71J/1/16"))
    }

    @Test
    fun `only anidub urls are recognised`() {
        assertTrue(isAniDubSourceUrl("source://anidub/12254-blich/1"))
        assertFalse(isAniDubSourceUrl("source://aniboom/x/1/1"))
    }
}
