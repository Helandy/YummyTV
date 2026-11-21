package su.afk.yummy.tv.source.alloha.data.extractor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class AllohaErrorPageTest : BaseUnitTest() {

    @Test
    fun `reason is the visible text of the stub without the title`() {
        val html = """
            <!DOCTYPE html><html><head><title>Ошибка!</title>
            <style>.error { display: flex; }</style></head>
            <body><div class="error"><h1>Ошибка!</h1>
            <p>Извините, указанная озвучка недоступна. Попробуйте выбрать другую озвучку</p></div></body></html>
        """.trimIndent()

        assertEquals(
            "Извините, указанная озвучка недоступна. Попробуйте выбрать другую озвучку",
            allohaErrorPageReason(html),
        )
    }

    @Test
    fun `reason is cut to a short length`() {
        val html = "<p>" + "а".repeat(500) + "</p>"

        assertEquals(100, allohaErrorPageReason(html)?.length)
    }

    @Test
    fun `page without text has no reason`() {
        assertNull(allohaErrorPageReason("<html><style>p { color: red }</style><body></body></html>"))
    }
}
