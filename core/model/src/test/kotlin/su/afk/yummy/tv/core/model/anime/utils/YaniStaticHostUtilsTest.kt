package su.afk.yummy.tv.core.model.anime.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class YaniStaticHostUtilsTest : BaseUnitTest() {

    @Test
    fun `invented static host is replaced with the real one`() {
        assertEquals(
            "https://static.yani.tv/posters/mega/1.avif?v=2",
            normalizeYaniStaticUrl("https://static-ru.yani.tv/posters/mega/1.avif?v=2"),
        )
        assertEquals(
            "http://static.yani.tv/users/full/5.avif",
            normalizeYaniStaticUrl("http://STATIC2.yani.tv/users/full/5.avif"),
        )
    }

    @Test
    fun `real and foreign urls stay untouched`() {
        listOf(
            "https://static.yani.tv/posters/mega/1.avif",
            "https://api.yani.tv/anime",
            "https://static.afk.su/posters/mega/1.avif",
            "https://image.tmdb.org/t/p/w780/a.jpg",
            "https://static-ru.yani.tv.evil.example/a.jpg",
        ).forEach { assertEquals(it, normalizeYaniStaticUrl(it)) }
    }
}
