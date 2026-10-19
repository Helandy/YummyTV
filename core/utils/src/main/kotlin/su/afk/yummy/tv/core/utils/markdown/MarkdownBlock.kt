package su.afk.yummy.tv.core.utils.markdown

/** Блок разобранного markdown (см. [parseMarkdown]). */
sealed interface MarkdownBlock {
    data class Heading(val level: Int, val spans: List<MarkdownSpan>) : MarkdownBlock

    data class Paragraph(val spans: List<MarkdownSpan>) : MarkdownBlock

    /** Пункт маркированного списка; [depth] — уровень вложенности от 0. */
    data class Bullet(val depth: Int, val spans: List<MarkdownSpan>) : MarkdownBlock

    data class Numbered(val number: Int, val depth: Int, val spans: List<MarkdownSpan>) : MarkdownBlock

    data class Quote(val spans: List<MarkdownSpan>) : MarkdownBlock

    data class Code(val text: String) : MarkdownBlock

    data object Divider : MarkdownBlock
}
