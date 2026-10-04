package su.afk.yummy.tv.core.utils.formatting

private val HTML_TAG_REGEX = Regex("<[^>]+>")
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

/** Converts HTML markup to readable plain text: tags become spaces, entities are decoded. */
fun String.htmlToPlainText(): String =
    stripHtmlTags(" ")
        .decodeCommonHtmlEntities()
        .replace(WHITESPACE_REGEX, " ")
        .trim()
