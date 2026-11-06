package su.afk.yummy.tv.feature.details.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimePoster

class FullscreenPosterUtilsTest {

    private val poster = AnimePoster(
        small = "small",
        medium = "medium",
        big = "big",
        fullsize = "full",
        mega = "mega",
    )

    @Test
    fun `fullscreen poster is mega`() {
        assertEquals("mega", poster.fullscreenUrl())
    }

    @Test
    fun `no mega gives no fullscreen poster`() {
        assertNull(poster.copy(mega = null).fullscreenUrl())
    }
}
