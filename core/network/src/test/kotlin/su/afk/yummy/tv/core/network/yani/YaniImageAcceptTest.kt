package su.afk.yummy.tv.core.network.yani

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YaniImageAcceptTest {

    @Test
    fun `avif is requested when the device decodes it`() {
        assertEquals("image/avif", yaniImageAccept(avifSupported = true))
    }

    @Test
    fun `no header without avif support, webp is never requested`() {
        assertNull(yaniImageAccept(avifSupported = false))
    }
}
