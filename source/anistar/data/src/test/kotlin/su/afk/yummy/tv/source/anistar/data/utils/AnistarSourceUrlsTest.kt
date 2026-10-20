package su.afk.yummy.tv.source.anistar.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTarget

class AnistarSourceUrlsTest {
    @Test
    fun `url round trips`() {
        val target = AnistarTarget(2658, 391)

        assertEquals("source://anistar/2658/391", target.toSourceUrl())
        assertEquals(target, parseAnistarSourceUrl(target.toSourceUrl()))
    }

    @Test
    fun `prefix check ignores case and rejects other sources`() {
        assertTrue(isAnistarSourceUrl("SOURCE://AniStar/1/1"))
        assertFalse(isAnistarSourceUrl("source://aniboom/1/1/1"))
    }

    @Test
    fun `malformed urls are rejected`() {
        assertNull(parseAnistarSourceUrl("source://anistar/2658"))
        assertNull(parseAnistarSourceUrl("source://anistar/2658/1/9"))
        assertNull(parseAnistarSourceUrl("source://anistar/x/1"))
        assertNull(parseAnistarSourceUrl("source://anistar/2658/0"))
        assertNull(parseAnistarSourceUrl("https://v31.astar.bz/2658/1"))
    }
}
