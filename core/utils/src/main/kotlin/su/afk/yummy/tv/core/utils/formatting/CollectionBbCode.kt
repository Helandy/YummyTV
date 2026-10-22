package su.afk.yummy.tv.core.utils.formatting

/** Стиль фрагмента BB-разметки описания коллекции. */
enum class BbStyle { Bold, Italic, Underline }

/** Фрагмент текста с набором применённых к нему стилей. */
data class BbSpan(
    val text: String,
    val styles: Set<BbStyle> = emptySet(),
)

private val tagRegex = Regex("\\[(/?)(ж|к|п|спойлер(?:=[^\\]]*)?)]", RegexOption.IGNORE_CASE)

private fun String.toBbStyle(): BbStyle? = when (lowercase()) {
    "ж" -> BbStyle.Bold
    "к" -> BbStyle.Italic
    "п" -> BbStyle.Underline
    else -> null
}

/**
 * Разбирает BB-коды описания коллекции: `[ж]` — жирный, `[к]` — курсив, `[п]` — подчёркнутый.
 * Теги спойлера снимаются, содержимое остаётся. Незакрытые теги действуют до конца текста,
 * лишние закрывающие игнорируются, остальные квадратные скобки остаются текстом.
 */
fun parseBbCode(text: String): List<BbSpan> {
    val source = text.replace("&nbsp;", " ")
    val spans = mutableListOf<BbSpan>()
    val open = mutableMapOf<BbStyle, Int>()
    var cursor = 0

    fun flush(end: Int) {
        if (end > cursor) spans += BbSpan(source.substring(cursor, end), open.filterValues { it > 0 }.keys)
    }

    tagRegex.findAll(source).forEach { match ->
        flush(match.range.first)
        cursor = match.range.last + 1
        val style = match.groupValues[2].toBbStyle() ?: return@forEach
        val depth = open[style] ?: 0
        open[style] = if (match.groupValues[1].isEmpty()) depth + 1 else (depth - 1).coerceAtLeast(0)
    }
    flush(source.length)
    return spans
}
