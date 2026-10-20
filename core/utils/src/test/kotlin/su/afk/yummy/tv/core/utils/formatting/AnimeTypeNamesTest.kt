package su.afk.yummy.tv.core.utils.formatting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class AnimeTypeNamesTest : BaseUnitTest() {

    @Test
    fun `full length movie is shortened in every language`() {
        assertEquals("Фильм", "Полнометражный фильм".toShortAnimeTypeName())
        assertEquals("Фільм", "Повнометражний фільм".toShortAnimeTypeName())
        assertEquals("Movie", "Full-length movie".toShortAnimeTypeName())
    }

    @Test
    fun `case and surrounding spaces do not matter`() {
        assertEquals("Фильм", "  полнометражный ФИЛЬМ ".toShortAnimeTypeName())
    }

    @Test
    fun `other types and null stay as they are`() {
        assertEquals("ТВ Сериал", "ТВ Сериал".toShortAnimeTypeName())
        assertNull(null.toShortAnimeTypeName())
    }
}
