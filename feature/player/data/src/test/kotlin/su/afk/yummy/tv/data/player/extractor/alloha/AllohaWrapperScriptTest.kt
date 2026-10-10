package su.afk.yummy.tv.data.player.extractor.alloha

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

/**
 * Контракт моста: JS вызывает `AndroidBridge.<имя>`, а Kotlin-сторона объявляет эти методы как
 * `@JavascriptInterface` в `AllohaExtractor` (анонимный объект внутри suspend-лямбды, рефлексией
 * его не достать). Список методов продублирован в [BRIDGE_METHODS] намеренно: тест падает, если
 * скрипт начал вызывать метод, которого на стороне Kotlin нет.
 */
class AllohaWrapperScriptTest : BaseUnitTest() {

    @Test
    fun `iframe url is html escaped into the src attribute`() {
        val html = wrapperHtml("https://alloha.example/embed?a=1&b=\"2\"")

        assertTrue(html.contains("""src="https://alloha.example/embed?a=1&amp;b=&quot;2&quot;""""))
    }

    @Test
    fun `wrapper contains the alloha iframe`() {
        assertTrue(wrapperHtml("https://a/b").contains("""<iframe id="alloha""""))
    }

    @Test
    fun `bridge name matches what the script calls`() {
        assertEquals("AndroidBridge", BRIDGE_NAME)
        assertTrue(wrapperHtml("https://a/b").contains("$BRIDGE_NAME."))
    }

    @Test
    fun `script only calls bridge methods the kotlin side declares`() {
        val called = Regex("""AndroidBridge\.([A-Za-z0-9_]+)""")
            .findAll(wrapperHtml("https://a/b"))
            .map { it.groupValues[1] }
            .toSet()

        assertTrue("script calls no bridge methods", called.isNotEmpty())
        val unknown = called - BRIDGE_METHODS
        assertTrue("script calls undeclared bridge methods: $unknown", unknown.isEmpty())
    }

    private companion object {
        val BRIDGE_METHODS = setOf(
            "onReady",
            "onConfigUpdate",
            "onM3u8Refreshed",
            "onStreamHeaders",
            "onDubbingUnavailable",
            "onLog",
        )
    }
}
