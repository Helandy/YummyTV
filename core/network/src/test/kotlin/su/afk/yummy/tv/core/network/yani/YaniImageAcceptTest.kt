package su.afk.yummy.tv.core.network.yani

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YaniImageAcceptTest {

    @Test
    fun `avif and webp are listed through a comma`() {
        assertEquals("image/avif,image/webp", yaniImageAccept(avifSupported = true, webpSupported = true))
    }

    @Test
    fun `only supported formats are listed`() {
        assertEquals("image/avif", yaniImageAccept(avifSupported = true, webpSupported = false))
        assertEquals("image/webp", yaniImageAccept(avifSupported = false, webpSupported = true))
    }

    @Test
    fun `no header when device supports neither`() {
        assertNull(yaniImageAccept(avifSupported = false, webpSupported = false))
    }
}
