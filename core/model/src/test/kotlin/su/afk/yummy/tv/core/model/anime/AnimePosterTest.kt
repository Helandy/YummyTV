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
    fun `only the version of the chosen quality is returned`() {
        assertEquals(listOf("medium"), poster.urlsByQuality(PosterQuality.LOW))
        assertEquals(listOf("big"), poster.urlsByQuality(PosterQuality.STANDARD))
        assertEquals(listOf("mega"), poster.urlsByQuality(PosterQuality.MEGA))
        assertEquals(listOf("fullsize"), poster.urlsByQuality(PosterQuality.HIGH))
    }

    @Test
    fun `missing or blank chosen version gives no urls`() {
        val sparse = AnimePoster(small = "small", medium = " ", big = "big", fullsize = null, mega = null)

        assertEquals(emptyList<String>(), sparse.urlsByQuality(PosterQuality.MEGA))
        assertEquals(emptyList<String>(), sparse.urlsByQuality(PosterQuality.LOW))
        assertEquals(emptyList<String>(), sparse.urlsByQuality(PosterQuality.HIGH))
        assertEquals(listOf("big"), sparse.urlsByQuality(PosterQuality.STANDARD))
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
