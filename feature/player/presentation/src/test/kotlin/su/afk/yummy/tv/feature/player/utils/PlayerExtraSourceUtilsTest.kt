package su.afk.yummy.tv.feature.player.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerExtraSourceUtilsTest {
    @Test
    fun `aggregator source url adds its name to the player`() {
        assertEquals("Ashdi (Mikai)", "Ashdi".withExtraSourceName("source://mikai/ashdi/221709"))
        assertEquals("Fenix (AnimeON)", "Fenix".withExtraSourceName("source://animeon/924/1/1093/9061"))
    }

    @Test
    fun `other urls and blank names stay as is`() {
        assertEquals("AniBoom", "AniBoom".withExtraSourceName("source://aniboom/1/2"))
        assertEquals("Kodik", "Kodik".withExtraSourceName("//kodik.info/seria/1"))
        assertEquals("", "".withExtraSourceName("source://mikai/ashdi/1"))
    }
}
