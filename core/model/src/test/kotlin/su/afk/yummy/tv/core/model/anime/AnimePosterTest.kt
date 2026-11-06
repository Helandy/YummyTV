package su.afk.yummy.tv.core.model.anime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class AnimePosterTest : BaseUnitTest() {

    private val poster = AnimePoster(
        small = "small",
        medium = "medium",
        big = "big",
        fullsize = "fullsize",
        mega = "mega",
    )

    @Test
    fun `only the mega version is returned for display`() {
        assertEquals(listOf("mega"), poster.posterUrls())
    }

    @Test
    fun `missing or blank mega gives no urls`() {
        val sparse = AnimePoster(small = "small", medium = "medium", big = "big", fullsize = "fullsize", mega = " ")

        assertEquals(emptyList<String>(), sparse.posterUrls())
        assertEquals(emptyList<String>(), sparse.copy(mega = null).posterUrls())
    }

    @Test
    fun `default url is the mega version`() {
        assertEquals("mega", poster.defaultUrl())
        assertNull(poster.copy(mega = null).defaultUrl())
        assertNull(poster.copy(mega = " ").defaultUrl())
    }

    @Test
    fun `null poster gives no urls`() {
        val none: AnimePoster? = null

        assertEquals(emptyList<String>(), none.posterUrls())
        assertNull(none.defaultUrl())
    }

    @Test
    fun `yani mega avif gets a mirror on our host with the same query`() {
        val avif = AnimePoster(null, null, null, null, "https://static.yani.tv/posters/mega/1636989985.avif?v=431865090")

        assertEquals(
            listOf(
                "https://static.yani.tv/posters/mega/1636989985.avif?v=431865090",
                "https://static.self.made/posters/mega/1636989985.avif?v=431865090",
            ),
            avif.posterUrls(),
        )
        assertEquals("https://static.yani.tv/posters/mega/1636989985.avif?v=431865090", avif.defaultUrl())
    }

    @Test
    fun `jpeg mega substituted by yani has no mirror`() {
        val jpg = AnimePoster(null, null, null, null, "https://static.yani.tv/posters/full/1.jpg")

        assertEquals(listOf("https://static.yani.tv/posters/full/1.jpg"), jpg.posterUrls())
    }
}
