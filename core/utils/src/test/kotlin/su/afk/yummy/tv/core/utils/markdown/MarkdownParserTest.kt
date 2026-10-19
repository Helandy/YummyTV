package su.afk.yummy.tv.core.utils.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class MarkdownParserTest : BaseUnitTest() {

    @Test
    fun `heading and bullets of a typical release body`() {
        val blocks = "### Что новенького?\n- Первое\n- Второе  ".parseMarkdown()

        assertEquals(
            listOf(
                MarkdownBlock.Heading(3, listOf(MarkdownSpan("Что новенького?"))),
                MarkdownBlock.Bullet(0, listOf(MarkdownSpan("Первое"))),
                MarkdownBlock.Bullet(0, listOf(MarkdownSpan("Второе"))),
            ),
            blocks,
        )
    }

    @Test
    fun `nested and numbered lists keep depth and number`() {
        val blocks = "* a\n  * b\n2. c".parseMarkdown()

        assertEquals(MarkdownBlock.Bullet(0, listOf(MarkdownSpan("a"))), blocks[0])
        assertEquals(MarkdownBlock.Bullet(1, listOf(MarkdownSpan("b"))), blocks[1])
        assertEquals(MarkdownBlock.Numbered(2, 0, listOf(MarkdownSpan("c"))), blocks[2])
    }

    @Test
    fun `inline styles links and code`() {
        val spans = parseInline("a **b** *c* ~~d~~ `e` [f](http://x.y)")

        assertEquals(
            listOf(
                MarkdownSpan("a "),
                MarkdownSpan("b", bold = true),
                MarkdownSpan(" "),
                MarkdownSpan("c", italic = true),
                MarkdownSpan(" "),
                MarkdownSpan("d", strike = true),
                MarkdownSpan(" "),
                MarkdownSpan("e", code = true),
                MarkdownSpan(" "),
                MarkdownSpan("f", link = "http://x.y"),
            ),
            spans,
        )
    }

    @Test
    fun `bare url loses trailing punctuation and lone asterisk stays literal`() {
        val spans = parseInline("2 * 3 see https://a.b/c.")

        assertEquals("2 * 3 see ", spans.first().text)
        assertEquals(MarkdownSpan("https://a.b/c", link = "https://a.b/c"), spans[1])
        assertEquals(MarkdownSpan("."), spans[2])
    }

    @Test
    fun `images and html tags are dropped and checkboxes get marks`() {
        val blocks = "![pic](http://i.png) <b>x</b>\n- [x] done\n- [ ] todo".parseMarkdown()

        assertEquals(MarkdownBlock.Paragraph(listOf(MarkdownSpan("x"))), blocks[0])
        assertEquals("☑ done", (blocks[1] as MarkdownBlock.Bullet).spans.joinToString("") { it.text })
        assertEquals("☐ todo", (blocks[2] as MarkdownBlock.Bullet).spans.joinToString("") { it.text })
    }

    @Test
    fun `code fence quote and divider`() {
        val blocks = "> цитата\n---\n```\nval a = 1\n```".parseMarkdown()

        assertEquals(MarkdownBlock.Quote(listOf(MarkdownSpan("цитата"))), blocks[0])
        assertEquals(MarkdownBlock.Divider, blocks[1])
        assertEquals(MarkdownBlock.Code("val a = 1"), blocks[2])
    }

    @Test
    fun `blank input gives no blocks`() {
        assertTrue("  \n\n ".parseMarkdown().isEmpty())
    }
}
