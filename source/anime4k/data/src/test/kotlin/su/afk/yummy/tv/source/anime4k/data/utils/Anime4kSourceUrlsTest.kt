package su.afk.yummy.tv.source.anime4k.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTarget

class Anime4kSourceUrlsTest : BaseUnitTest() {

    @Test
    fun `round trip keeps cyrillic spaces and slashes in team`() {
        val target = Anime4kTarget(1735, season = "1", episode = "01", team = "Мосфильм Мастер / A+B")

        val url = target.toSourceUrl()

        assertTrue(url.startsWith("source://anime4k/1735/1/01/"))
        assertFalse("слэши внутри озвучки должны быть закодированы", url.removePrefix("source://anime4k/").count { it == '/' } > 3)
        assertEquals(target, parseAnime4kSourceUrl(url))
    }

    @Test
    fun `empty season is preserved`() {
        val target = Anime4kTarget(5, season = "", episode = "3", team = "X")

        assertEquals(target, parseAnime4kSourceUrl(target.toSourceUrl()))
    }

    @Test
    fun `foreign or broken urls are rejected`() {
        assertFalse(isAnime4kSourceUrl("https://kodik.info/x"))
        assertNull(parseAnime4kSourceUrl("https://kodik.info/x"))
        assertNull(parseAnime4kSourceUrl("source://anime4k/abc/1/1/X"))
        assertNull(parseAnime4kSourceUrl("source://anime4k/5/1/1"))
        assertNull(parseAnime4kSourceUrl("source://anime4k/5/1/1/"))
    }
}
