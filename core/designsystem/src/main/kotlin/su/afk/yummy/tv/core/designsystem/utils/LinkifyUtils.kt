package su.afk.yummy.tv.core.designsystem.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles

private val webUrlRegex = Regex("""https?://[^\s<>"']+""")
private val emailRegex = Regex("""[\w.+-]+@[\w-]+(\.[\w-]+)+""")
private const val TRAILING_PUNCTUATION = ".,;:!?)»"

/**
 * Делает http(s)-ссылки и почтовые адреса в уже добавленном [text] кликабельными
 * ([LinkAnnotation.Url], почта — через mailto). [text] должен совпадать с содержимым билдера с нуля.
 * Знаки препинания в конце ссылки («…/support.») в неё не входят.
 */
fun AnnotatedString.Builder.addUrlLinks(text: String, linkStyle: SpanStyle) {
    urlRanges(text).forEach { (url, range) ->
        addLink(LinkAnnotation.Url(url = url, styles = TextLinkStyles(style = linkStyle)), range.first, range.last + 1)
    }
}

/**
 * Только подсвечивает ссылки и почту в [text], не делая их кликабельными. Для ТВ: [LinkAnnotation] фокусируется
 * с пульта, но фокус на ней не виден, и стрелки «застревают» на тексте.
 */
fun AnnotatedString.Builder.addUrlHighlights(text: String, linkStyle: SpanStyle) {
    urlRanges(text).forEach { (_, range) -> addStyle(linkStyle, range.first, range.last + 1) }
}

/** Адрес ссылки (почта — с mailto) и её диапазон в [text]. */
private fun urlRanges(text: String): List<Pair<String, IntRange>> {
    val webRanges = webUrlRegex.findAll(text).toList()
    val web = webRanges.mapNotNull { match ->
        val url = match.value.trimEnd { it in TRAILING_PUNCTUATION }
        url.takeIf { it.isNotEmpty() }?.let { it to (match.range.first until match.range.first + it.length) }
    }
    val emails = emailRegex.findAll(text)
        .filterNot { email -> webRanges.any { email.range.first in it.range } }
        .map { "mailto:${it.value}" to it.range }
    return web + emails
}
