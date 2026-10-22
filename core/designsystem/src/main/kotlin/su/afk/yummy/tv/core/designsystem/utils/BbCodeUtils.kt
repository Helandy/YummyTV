package su.afk.yummy.tv.core.designsystem.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import su.afk.yummy.tv.core.utils.formatting.BbStyle
import su.afk.yummy.tv.core.utils.formatting.parseBbCode

private fun Set<BbStyle>.toSpanStyle(): SpanStyle = SpanStyle(
    fontWeight = if (BbStyle.Bold in this) FontWeight.Bold else null,
    fontStyle = if (BbStyle.Italic in this) FontStyle.Italic else null,
    textDecoration = if (BbStyle.Underline in this) TextDecoration.Underline else null,
)

/**
 * Превращает описание с BB-кодами (`[ж]`, `[к]`, `[п]`) в [AnnotatedString]. С [linkStyle] ссылки
 * в тексте кликабельны — на ТВ его не передают: ссылка там недоступна для фокуса. [compact] убирает
 * пустые строки целиком (для свёрнутого текста, где они съедали бы строки лимита); иначе подряд
 * идущие пустые строки схлопываются до одной.
 */
fun String.toBbAnnotatedString(linkStyle: SpanStyle? = null, compact: Boolean = false): AnnotatedString {
    val spans = parseBbCode(this.squeezeBlankLines(compact))
    return buildAnnotatedString {
        spans.forEach { span ->
            if (span.styles.isEmpty()) append(span.text) else withStyle(span.styles.toSpanStyle()) { append(span.text) }
        }
        if (linkStyle != null) addUrlLinks(spans.joinToString("") { it.text }, linkStyle)
    }
}

private val paragraphBreakRegex = Regex("\\n+")

/** Делит текст на абзацы по переводам строки, сохраняя разметку; пустые абзацы отбрасываются. */
fun AnnotatedString.splitParagraphs(): List<AnnotatedString> {
    val result = mutableListOf<AnnotatedString>()
    var cursor = 0
    fun add(end: Int) {
        val part = subSequence(cursor, end)
        val trimmedStart = part.text.indexOfFirst { !it.isWhitespace() }
        if (trimmedStart < 0) return
        val trimmedEnd = part.text.indexOfLast { !it.isWhitespace() } + 1
        result += part.subSequence(trimmedStart, trimmedEnd)
    }
    paragraphBreakRegex.findAll(text).forEach { match ->
        add(match.range.first)
        cursor = match.range.last + 1
    }
    add(length)
    return result
}

private val manyNewlinesRegex = Regex("\\n{3,}")
private val anyBlankLinesRegex = Regex("\\n\\s*\\n+")

private fun String.squeezeBlankLines(compact: Boolean): String =
    if (compact) replace(anyBlankLinesRegex, "\n") else replace(manyNewlinesRegex, "\n\n")
