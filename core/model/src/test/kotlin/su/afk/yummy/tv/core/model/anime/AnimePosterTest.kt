package su.afk.yummy.tv.core.model.anime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.utils.yaniPosterCacheKey
import su.afk.yummy.tv.core.model.anime.utils.yaniStaticPosterFallback
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
    fun `jpeg mega substituted by yani gets the avif mirror of the same id`() {
        val jpg = AnimePoster(null, null, null, null, "https://static.yani.tv/posters/full/1.jpg?v=3")

        assertEquals(
            listOf("https://static.yani.tv/posters/full/1.jpg?v=3", "https://static.self.made/posters/mega/1.avif?v=3"),
            jpg.posterUrls(),
        )
    }

    @Test
    fun `mirror is built for any host and the invented one is corrected first`() {
        val other = AnimePoster(null, null, null, null, "https://cdn.example.org/posters/mega/7.avif")
        val invented = AnimePoster(null, null, null, null, "https://static-ru.yani.tv/posters/mega/7.avif?v=1")

        assertEquals(
            listOf("https://cdn.example.org/posters/mega/7.avif", "https://static.self.made/posters/mega/7.avif"),
            other.posterUrls(),
        )
        assertEquals(
            listOf("https://static.yani.tv/posters/mega/7.avif?v=1", "https://static.self.made/posters/mega/7.avif?v=1"),
            invented.posterUrls(),
        )
    }

    @Test
    fun `a link to our own host gets no second mirror`() {
        val own = AnimePoster(null, null, null, null, "https://static.self.made/posters/mega/7.avif")

        assertEquals(listOf("https://static.self.made/posters/mega/7.avif"), own.posterUrls())
    }

    @Test
    fun `api host goes first then static yani then our mirror`() {
        val proxied = AnimePoster(null, null, null, null, "https://imgproxy.yani.tv/posters/mega/9.avif?v=5")

        assertEquals(
            listOf(
                "https://imgproxy.yani.tv/posters/mega/9.avif?v=5",
                "https://static.yani.tv/posters/mega/9.avif?v=5",
                "https://static.self.made/posters/mega/9.avif?v=5",
            ),
            proxied.posterUrls(),
        )
    }

    @Test
    fun `jpeg from another yani host goes through static yani then the avif mirror`() {
        val jpg = AnimePoster(null, null, null, null, "https://imgproxy.yani.tv/posters/full/9.jpg")

        assertEquals(
            listOf(
                "https://imgproxy.yani.tv/posters/full/9.jpg",
                "https://static.yani.tv/posters/full/9.jpg",
                "https://static.self.made/posters/mega/9.avif",
            ),
            jpg.posterUrls(),
        )
    }

    @Test
    fun `api host and foreign hosts get no static yani step`() {
        assertEquals(null, yaniStaticPosterFallback("https://api.yani.tv/posters/mega/9.avif"))
        assertEquals(null, yaniStaticPosterFallback("https://static.yani.tv/posters/mega/9.avif"))
        assertEquals(null, yaniStaticPosterFallback("https://cdn.example.org/posters/mega/9.avif"))
        assertEquals(null, yaniStaticPosterFallback("https://imgproxy.yani.tv/other/9.avif"))
    }

    @Test
    fun `cache key depends only on the poster id`() {
        val key = "yani-poster:9"

        assertEquals(key, yaniPosterCacheKey("https://imgproxy.yani.tv/posters/mega/9.avif?v=5"))
        assertEquals(key, yaniPosterCacheKey("https://static.yani.tv/posters/full/9.jpg"))
        assertEquals(key, yaniPosterCacheKey("https://static.self.made/posters/mega/9.avif"))
        assertNull(yaniPosterCacheKey("https://image.tmdb.org/t/p/w300/abc.jpg"))
        assertNull(yaniPosterCacheKey("https://static.self.made/img/still/abc.avif"))
    }
}
