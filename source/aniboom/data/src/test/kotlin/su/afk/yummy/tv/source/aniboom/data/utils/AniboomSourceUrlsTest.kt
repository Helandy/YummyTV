package su.afk.yummy.tv.source.aniboom.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTarget

class AniboomSourceUrlsTest : BaseUnitTest() {

    @Test
    fun `url round trips`() {
        val target = AniboomTarget("0BzM07Pq71J", 12, 16)

        assertEquals("source://aniboom/0BzM07Pq71J/12/16", target.toSourceUrl())
        assertEquals(target, parseAniboomSourceUrl(target.toSourceUrl()))
    }

    @Test
    fun `foreign or malformed urls are rejected`() {
        assertFalse(isAniboomSourceUrl("source://anime4k/1/1/1/x"))
        assertTrue(isAniboomSourceUrl("source://aniboom/0BzM07Pq71J/1/1"))
        assertNull(parseAniboomSourceUrl("source://aniboom/0BzM07Pq71J/1"))
        assertNull(parseAniboomSourceUrl("source://aniboom/short/1/1"))
        assertNull(parseAniboomSourceUrl("source://aniboom/0BzM07Pq71J/0/1"))
        assertNull(parseAniboomSourceUrl("source://aniboom/0BzM07Pq71J/1/x"))
        assertNull(parseAniboomSourceUrl("source://aniboom/0BzM07Pq71J/1/1/extra"))
    }

    @Test
    fun `embed url has no parent and carries episode and translation`() {
        assertEquals(
            "https://aniboom.one/embed/0BzM07Pq71J?episode=3&translation=16&episodes=1",
            aniboomEmbedUrl("0BzM07Pq71J", 3, 16),
        )
    }
}
