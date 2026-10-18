package su.afk.yummy.tv.data.account.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class BannerUrlUtilsTest : BaseUnitTest() {

    @Test
    fun `cropped banner maps to full avif keeping version`() {
        assertEquals(
            "https://static.yani.tv/banners/full/1.avif?v=431865090",
            "https://static.yani.tv/banners/cropped/1.webp?v=431865090".toWideBannerUrl(),
        )
    }

    @Test
    fun `cropped banner without query maps too`() {
        assertEquals(
            "https://static.yani.tv/banners/full/42.avif",
            "https://static.yani.tv/banners/cropped/42.webp".toWideBannerUrl(),
        )
    }

    @Test
    fun `non cropped url has no wide version`() {
        assertNull("https://static.yani.tv/users/full/1.avif".toWideBannerUrl())
    }
}
