package su.afk.yummy.tv.core.model.anime.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class AnimeTitleUtilsTest : BaseUnitTest() {

    @Test
    fun `latin titles go first then cyrillic then others and limit applies after sorting`() {
        val result = prioritizeAnimeTitles(
            title = "Фрирен",
            otherTitles = listOf("葬送のフリーレン", "Провожающая в последний путь Фрирен", " Sousou no Frieren ", "Frieren: Beyond Journey's End", "фрирен"),
            limit = 3,
        )

        assertEquals(listOf("Sousou no Frieren", "Frieren: Beyond Journey's End", "Провожающая в последний путь Фрирен"), result)
    }

    @Test
    fun `duplicates by key and the main title are dropped`() {
        val result = prioritizeAnimeTitles("Bleach", listOf("BLEACH", "Bleach!", "Блич", "блич"), 10)

        assertEquals(listOf("Блич"), result)
    }

    @Test
    fun `original title is the first latin one`() {
        assertEquals("Sousou no Frieren", originalAnimeTitle(listOf("Фрирен", "Sousou no Frieren", "Frieren")))
        assertNull(originalAnimeTitle(listOf("Фрирен", "葬送のフリーレン")))
    }

    @Test
    fun `season suffixes are stripped`() {
        assertEquals("Mushoku Tensei", stripSeasonSuffix("Mushoku Tensei Season 2"))
        assertEquals("Re:Zero", stripSeasonSuffix("Re:Zero 2nd Season"))
        assertEquals("Атака титанов", stripSeasonSuffix("Атака титанов 3 сезон"))
        assertEquals("Overlord", stripSeasonSuffix("Overlord Part 2"))
        assertEquals("Naruto", stripSeasonSuffix("Naruto"))
    }
}
