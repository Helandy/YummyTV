package su.afk.yummy.tv.source.ailiberty.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.source.ailiberty.domain.model.AiLibertyTarget

class AiLibertySourceUrlsTest {

    @Test
    fun `target round trips through the url`() {
        val target = AiLibertyTarget("tensei-shitara-slime-datta-ken-3rd-season", 7)

        assertEquals("source://ailiberty/tensei-shitara-slime-datta-ken-3rd-season/7", target.toSourceUrl())
        assertEquals(target, parseAiLibertySourceUrl(target.toSourceUrl()))
    }

    @Test
    fun `malformed urls are rejected`() {
        assertNull(parseAiLibertySourceUrl("source://ailiberty/psyren"))
        assertNull(parseAiLibertySourceUrl("source://ailiberty/psyren/0"))
        assertNull(parseAiLibertySourceUrl("source://ailiberty/psyren/x"))
        assertNull(parseAiLibertySourceUrl("source://ailiberty/../etc/1"))
        assertNull(parseAiLibertySourceUrl("source://aniboom/0BzM07Pq71J/1/16"))
    }

    @Test
    fun `only ailiberty urls are recognised`() {
        assertTrue(isAiLibertySourceUrl("source://ailiberty/psyren/1"))
        assertFalse(isAiLibertySourceUrl("source://aniboom/x/1/1"))
    }
}
