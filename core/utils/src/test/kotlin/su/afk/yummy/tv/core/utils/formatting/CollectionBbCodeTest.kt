package su.afk.yummy.tv.core.utils.formatting

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class CollectionBbCodeTest : BaseUnitTest() {

    @Test
    fun `nested tags combine styles`() {
        assertEquals(
            listOf(BbSpan("Коллекция", setOf(BbStyle.Underline, BbStyle.Bold)), BbSpan("\nконец")),
            parseBbCode("[п][ж]Коллекция[/ж][/п]\nконец"),
        )
    }

    @Test
    fun `bold and italic are separate spans`() {
        assertEquals(
            listOf(BbSpan("a", setOf(BbStyle.Bold)), BbSpan("b"), BbSpan("c", setOf(BbStyle.Italic))),
            parseBbCode("[ж]a[/ж]b[к]c[/к]"),
        )
    }

    @Test
    fun `unclosed tag lasts to the end and stray closing tag is ignored`() {
        assertEquals(listOf(BbSpan("x"), BbSpan("y", setOf(BbStyle.Bold))), parseBbCode("[/ж]x[ж]y"))
    }

    @Test
    fun `spoiler tags are stripped and unknown brackets are kept`() {
        assertEquals(
            listOf(BbSpan("a [TV] b")),
            parseBbCode("[спойлер=\"Заголовок\"]a [TV] b[/спойлер]"),
        )
    }
}
