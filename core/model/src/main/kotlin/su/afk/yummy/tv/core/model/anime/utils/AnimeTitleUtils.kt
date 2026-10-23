package su.afk.yummy.tv.core.model.anime.utils

private val CYRILLIC = Regex("\\p{IsCyrillic}")
private val LATIN = Regex("\\p{IsLatin}")

// Границы слова и цифры заданы явными Unicode-классами: встроенный флаг `(?U)` Android ICU не принимает,
// а `\b` без него на JVM 19+ видит только ASCII.
private const val WORD_START = """(?<![\p{L}\p{N}_])"""
private const val WORD_END = """(?![\p{L}\p{N}_])"""
private const val PREFIX = """[\s:,\-–—]*"""
private const val NUMBER = """\p{Nd}+"""
private val SEASON_SUFFIXES = listOf(
    Regex("""$PREFIX$WORD_START(?:$NUMBER(?:st|nd|rd|th)|first|second|third|fourth|final)\s+season$WORD_END.*$""", RegexOption.IGNORE_CASE),
    Regex("""$PREFIX${WORD_START}season\s+$NUMBER$WORD_END.*$""", RegexOption.IGNORE_CASE),
    Regex("""$PREFIX$WORD_START(?:part|cour)\s+$NUMBER$WORD_END.*$""", RegexOption.IGNORE_CASE),
    Regex("""$PREFIX$WORD_START$NUMBER\s*(?:сезон|часть)$WORD_END.*$""", RegexOption.IGNORE_CASE),
    Regex("""$PREFIX$WORD_START(?:сезон|часть)\s+$NUMBER$WORD_END.*$""", RegexOption.IGNORE_CASE),
)

/** Ключ для сравнения названий: нижний регистр, «ё»→«е», только буквы и цифры. */
fun animeTitleKey(title: String): String = buildString(title.length) {
    for (ch in title.lowercase()) {
        val c = if (ch == 'ё') 'е' else ch
        if (c.isLetterOrDigit()) append(c)
    }
}

/** Латиница без кириллицы: ромадзи или английское название. */
fun isLatinTitle(title: String): Boolean = LATIN.containsMatchIn(title) && !CYRILLIC.containsMatchIn(title)

/**
 * Альтернативные названия в порядке полезности для поиска: сначала латиница (оригинал/ромадзи/английское),
 * затем кириллица, затем остальное (иероглифы). Порядок внутри группы сохраняется; повторы (в т.ч. совпадающие
 * с [title]) убираются, лимит применяется после сортировки.
 */
fun prioritizeAnimeTitles(title: String, otherTitles: List<String>, limit: Int): List<String> {
    val seen = hashSetOf(animeTitleKey(title))
    return otherTitles.asSequence()
        .map(String::trim)
        .filter { it.isNotEmpty() && seen.add(animeTitleKey(it)) }
        .sortedBy {
            if (isLatinTitle(it)) {
                0
            } else if (CYRILLIC.containsMatchIn(it)) {
                1
            } else {
                2
            }
        }
        .take(limit)
        .toList()
}

/** Первое латинское название — «оригинальное» для сервера; `null`, если латиницы нет. */
fun originalAnimeTitle(otherTitles: List<String>): String? =
    otherTitles.map(String::trim).firstOrNull { it.isNotEmpty() && isLatinTitle(it) }

/** Название без хвоста сезона/части («Foo 2nd Season» → «Foo»); исходное, если хвоста нет или остаток короче двух символов. */
fun stripSeasonSuffix(title: String): String {
    for (regex in SEASON_SUFFIXES) {
        val stripped = title.replace(regex, "").trim()
        if (stripped.length >= 2 && stripped != title.trim()) return stripped
    }
    return title.trim()
}
