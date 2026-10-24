package su.afk.yummy.tv.core.model.anime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.PosterQuality
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
    fun `urls follow the order of the chosen quality`() {
        assertEquals(
            listOf("medium", "big", "fullsize", "small", "mega"),
            poster.urlsByQuality(PosterQuality.LOW),
        )
        assertEquals(
            listOf("big", "medium", "fullsize", "small", "mega"),
            poster.urlsByQuality(PosterQuality.STANDARD),
        )
        assertEquals(
            listOf("mega", "big", "medium", "fullsize", "small"),
            poster.urlsByQuality(PosterQuality.MEGA),
        )
        assertEquals(
            listOf("fullsize", "mega", "big", "medium", "small"),
            poster.urlsByQuality(PosterQuality.HIGH),
        )
    }

    @Test
    fun `missing blank and duplicate urls are dropped`() {
        val sparse = AnimePoster(small = null, medium = " ", big = "same", fullsize = "same", mega = null)

        assertEquals(listOf("same"), sparse.urlsByQuality(PosterQuality.MEGA))
    }

    @Test
    fun `url for quality is the first preferred version`() {
        assertEquals("big", poster.urlFor(PosterQuality.STANDARD))
        assertEquals("mega", poster.urlFor(PosterQuality.MEGA))
    }

    @Test
    fun `null poster gives no urls`() {
        val none: AnimePoster? = null

        assertEquals(emptyList<String>(), none.urlsByQuality(PosterQuality.MEGA))
        assertNull(none.urlFor(PosterQuality.MEGA))
    }
}
