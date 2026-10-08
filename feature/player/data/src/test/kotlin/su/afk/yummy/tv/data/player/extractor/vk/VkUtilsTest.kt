package su.afk.yummy.tv.data.player.extractor.vk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import java.nio.charset.Charset

class VkUtilsTest : BaseUnitTest() {

    private fun page(message: String): ByteArray =
        """<center id="video_ext_wrap"><div id="video_ext_msg">
$message
</div><a id="video_ext_btn">VK</a></center>"""
            .toByteArray(Charset.forName("windows-1251"))

    @Test
    fun `message is read from windows-1251 bytes`() {
        assertEquals("Видеофайл не найден.", page("Видеофайл не найден.").vkUnavailableMessage())
    }

    @Test
    fun `tags and extra whitespace are stripped from the message`() {
        val bytes = page("Это видео <b>доступно</b>\n   только друзьям.")

        assertEquals("Это видео доступно только друзьям.", bytes.vkUnavailableMessage())
    }

    @Test
    fun `page without the message block has no message`() {
        assertNull("<html>player</html>".toByteArray().vkUnavailableMessage())
    }

    @Test
    fun `empty message block has no message`() {
        assertNull(page("   ").vkUnavailableMessage())
    }

    @Test
    fun `not found message is recognised ignoring case`() {
        assertTrue("Видеофайл не найден.".isVkVideoNotFoundMessage())
        assertTrue("ВИДЕОФАЙЛ НЕ НАЙДЕН".isVkVideoNotFoundMessage())
    }

    @Test
    fun `other vk messages are not treated as not found`() {
        assertFalse("Видео доступно только друзьям.".isVkVideoNotFoundMessage())
        assertFalse("Видео недоступно в вашем регионе.".isVkVideoNotFoundMessage())
    }
}
