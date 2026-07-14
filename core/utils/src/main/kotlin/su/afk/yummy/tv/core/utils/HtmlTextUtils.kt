package su.afk.yummy.tv.core.utils

private val HTML_TAG_REGEX = Regex("<[^>]+>")
private val HTML_ENTITY_REGEX = Regex("&[a-z0-9#]+;", RegexOption.IGNORE_CASE)
private val WHITESPACE_REGEX = Regex("\\s+")

/** Removes HTML tags like `<b>`, replacing each with [replacement]. */
fun String.stripHtmlTags(replacement: String = ""): String =
    replace(HTML_TAG_REGEX, replacement)

/** Decodes the small set of HTML entities that show up in API responses. */
fun String.decodeCommonHtmlEntities(): String =
    replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")

/** Drops any HTML entity (`&amp;`, `&#39;`, ...) outright, without decoding it. */
fun String.removeHtmlEntities(): String =
    replace(HTML_ENTITY_REGEX, "")

/** Converts HTML markup to readable plain text: tags become spaces, entities are decoded. */
fun String.htmlToPlainText(): String =
    stripHtmlTags(" ")
        .decodeCommonHtmlEntities()
        .replace(WHITESPACE_REGEX, " ")
        .trim()
