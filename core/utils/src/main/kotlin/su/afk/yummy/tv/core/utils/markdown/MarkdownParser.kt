package su.afk.yummy.tv.core.utils.markdown

private const val MAX_DEPTH = 3
private val headingRegex = Regex("""^(#{1,6})\s+(.*?)\s*#*\s*$""")
private val dividerRegex = Regex("""^(-{3,}|\*{3,}|_{3,})$""")
private val listRegex = Regex("""^(\s*)([-*+]|\d{1,9}[.)])\s+(.*)$""")
private val checkboxRegex = Regex("""^\[([ xX])]\s+(.*)$""")
private val htmlTagRegex = Regex("""</?[a-zA-Z][^<>]*>|<!--.*?-->""")
private val lineBreakRegex = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
private const val URL_TRAILING = ".,;:!?)"

/**
 * Разбирает markdown из описания GitHub-релиза в блоки: заголовки, абзацы, списки (с вложенностью,
 * нумерованные, чекбоксы), цитаты, блоки кода и разделители. Внутри строк понимает `**жирный**`,
 * `*курсив*`, `~~зачёркнутый~~`, `` `код` ``, `[ссылки](url)` и голые URL; картинки и HTML-теги
 * отбрасываются. Подряд идущие пустые строки схлопываются.
 */
fun String.parseMarkdown(): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val code = StringBuilder()
    var inCode = false

    for (raw in replace("\r\n", "\n").replace(lineBreakRegex, "\n").lines()) {
        val line = raw.trimEnd()
        val content = line.trimStart()

        if (content.startsWith("```") || content.startsWith("~~~")) {
            if (inCode) {
                blocks += MarkdownBlock.Code(code.toString().trimEnd('\n'))
                code.clear()
            }
            inCode = !inCode
            continue
        }
        if (inCode) {
            code.append(line).append('\n')
            continue
        }
        if (content.isEmpty()) continue

        blocks += parseLine(line, content) ?: continue
    }
    if (inCode && code.isNotBlank()) blocks += MarkdownBlock.Code(code.toString().trimEnd('\n'))
    return blocks
}

private fun parseLine(line: String, content: String): MarkdownBlock? {
    headingRegex.matchEntire(content)?.let { match ->
        val spans = parseInline(match.groupValues[2])
        return if (spans.isEmpty()) null else MarkdownBlock.Heading(match.groupValues[1].length, spans)
    }
    if (dividerRegex.matches(content)) return MarkdownBlock.Divider
    if (content.startsWith(">")) {
        val spans = parseInline(content.trimStart('>', ' '))
        return if (spans.isEmpty()) null else MarkdownBlock.Quote(spans)
    }
    listRegex.matchEntire(line)?.let { match ->
        val indent = match.groupValues[1].replace("\t", "    ").length
        val depth = (indent / 2).coerceAtMost(MAX_DEPTH)
        val marker = match.groupValues[2]
        var text = match.groupValues[3]
        var prefix = ""
        checkboxRegex.matchEntire(text)?.let { box ->
            prefix = if (box.groupValues[1] == " ") "☐ " else "☑ "
            text = box.groupValues[2]
        }
        val spans = parseInline(text).let { if (prefix.isEmpty()) it else listOf(MarkdownSpan(prefix)) + it }
        if (spans.isEmpty()) return null
        val number = marker.takeIf { it.first().isDigit() }?.dropLast(1)?.toIntOrNull()
        return if (number != null) MarkdownBlock.Numbered(number, depth, spans) else MarkdownBlock.Bullet(depth, spans)
    }
    val spans = parseInline(content)
    return if (spans.isEmpty()) null else MarkdownBlock.Paragraph(spans)
}

/** Инлайн-разметка одной строки; соседние фрагменты с одинаковым оформлением склеиваются. */
internal fun parseInline(text: String): List<MarkdownSpan> {
    val spans = mutableListOf<MarkdownSpan>()
    val buffer = StringBuilder()
    var bold = false
    var italic = false
    var strike = false

    fun flush(link: String? = null, code: Boolean = false, override: String? = null) {
        val value = override ?: buffer.toString()
        buffer.clear()
        if (value.isEmpty()) return
        spans += MarkdownSpan(value, bold = bold, italic = italic, strike = strike, code = code, link = link)
    }

    var i = 0
    while (i < text.length) {
        val c = text[i]
        val next = text.getOrNull(i + 1)
        val prev = text.getOrNull(i - 1)
        when {
            c == '\\' && next != null && next in "\\`*_{}[]()#+-.!~>" -> {
                buffer.append(next)
                i += 2
            }

            c == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    flush()
                    flush(code = true, override = text.substring(i + 1, end))
                    i = end + 1
                } else {
                    buffer.append(c)
                    i++
                }
            }

            c == '!' && next == '[' && imageEnd(text, i + 1) != -1 -> i = imageEnd(text, i + 1)

            c == '[' -> {
                val labelEnd = text.indexOf("](", i + 1)
                val urlEnd = if (labelEnd == -1) -1 else text.indexOf(')', labelEnd + 2)
                if (labelEnd != -1 && urlEnd != -1) {
                    flush()
                    val label = text.substring(i + 1, labelEnd).replace(Regex("""[*_`~]"""), "")
                    val url = text.substring(labelEnd + 2, urlEnd).trim().substringBefore(' ')
                    flush(link = url, override = label.ifEmpty { url })
                    i = urlEnd + 1
                } else {
                    buffer.append(c)
                    i++
                }
            }

            c == '<' && htmlTagRegex.matchAt(text, i) != null -> {
                i = htmlTagRegex.matchAt(text, i)!!.range.last + 1
            }

            text.startsWith("http://", i) || text.startsWith("https://", i) -> {
                var end = i
                while (end < text.length && !text[end].isWhitespace() && text[end] != '<') end++
                while (end > i && text[end - 1] in URL_TRAILING) end--
                flush()
                val url = text.substring(i, end)
                flush(link = url, override = url)
                i = end
            }

            text.startsWith("**", i) || text.startsWith("__", i) -> {
                if (bold || canOpen(text, i + 2)) {
                    flush()
                    bold = !bold
                    i += 2
                } else {
                    buffer.append(text, i, i + 2)
                    i += 2
                }
            }

            text.startsWith("~~", i) -> {
                if (strike || canOpen(text, i + 2)) {
                    flush()
                    strike = !strike
                    i += 2
                } else {
                    buffer.append("~~")
                    i += 2
                }
            }

            (c == '*' || c == '_') && (italic || canOpen(text, i + 1)) &&
                    (c == '*' || prev == null || !prev.isLetterOrDigit() || italic) -> {
                flush()
                italic = !italic
                i++
            }

            else -> {
                buffer.append(c)
                i++
            }
        }
    }
    flush()
    return spans.mergeNeighbours().trimEdges()
}

/** Индекс за концом `![alt](url)`, начинающегося с `[` в [bracket], либо -1. */
private fun imageEnd(text: String, bracket: Int): Int {
    val mid = text.indexOf("](", bracket)
    if (mid == -1) return -1
    val end = text.indexOf(')', mid + 2)
    return if (end == -1) -1 else end + 1
}

private fun canOpen(text: String, index: Int): Boolean {
    val ch = text.getOrNull(index) ?: return false
    return !ch.isWhitespace()
}

private fun List<MarkdownSpan>.mergeNeighbours(): List<MarkdownSpan> {
    val result = mutableListOf<MarkdownSpan>()
    for (span in this) {
        val last = result.lastOrNull()
        if (last != null && last.link == null && span.link == null &&
            last.copy(text = "") == span.copy(text = "")
        ) {
            result[result.lastIndex] = last.copy(text = last.text + span.text)
        } else {
            result += span
        }
    }
    return result
}

private fun List<MarkdownSpan>.trimEdges(): List<MarkdownSpan> {
    if (isEmpty()) return this
    val trimmed = toMutableList()
    trimmed[0] = trimmed[0].copy(text = trimmed[0].text.trimStart())
    trimmed[trimmed.lastIndex] = trimmed.last().copy(text = trimmed.last().text.trimEnd())
    return trimmed.filter { it.text.isNotEmpty() }
}
