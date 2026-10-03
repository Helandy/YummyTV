package su.afk.yummy.tv.feature.comments.utils

import su.afk.yummy.tv.feature.comments.model.CommentTextPart

private val spoilerRegex = Regex(
    pattern = "\\[спойлер(?:=\"([^\"]*)\")?](.*?)\\[/спойлер]",
    options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val bbCodeRegex = Regex("\\[/?[^\\]]+]")

/** Делит текст комментария на обычные фрагменты и спойлеры. */
fun splitSpoilers(
    text: String,
    defaultTitle: String,
): List<CommentTextPart> {
    val result = mutableListOf<CommentTextPart>()
    var cursor = 0
    spoilerRegex.findAll(text).forEach { match ->
        val before = text.substring(cursor, match.range.first)
        if (before.isNotBlank()) result += CommentTextPart.Plain(before)
        result += CommentTextPart.Spoiler(
            title = match.groups[1]?.value?.takeIf { it.isNotBlank() } ?: defaultTitle,
            text = match.groups[2]?.value.orEmpty(),
        )
        cursor = match.range.last + 1
    }
    val tail = text.substring(cursor)
    if (tail.isNotBlank()) result += CommentTextPart.Plain(tail)
    return result.ifEmpty { listOf(CommentTextPart.Plain(text)) }
}

/** Убирает BB-коды и неразрывные пробелы из текста комментария. */
fun String.stripBbCode(): String =
    replace(bbCodeRegex, "")
        .replace("&nbsp;", " ")
        .trim()

/** Делит текст на фрагменты и очищает каждый от BB-кодов. */
fun parseCommentText(text: String, defaultSpoilerTitle: String): List<CommentTextPart> =
    splitSpoilers(text, defaultSpoilerTitle).map { part ->
        when (part) {
            is CommentTextPart.Plain -> part.copy(text = part.text.stripBbCode())
            is CommentTextPart.Spoiler -> part.copy(text = part.text.stripBbCode())
        }
    }
