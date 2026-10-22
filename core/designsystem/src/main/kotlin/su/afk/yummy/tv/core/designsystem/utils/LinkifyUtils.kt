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
    val webRanges = webUrlRegex.findAll(text).toList()
    webRanges.forEach { match ->
        val url = match.value.trimEnd { it in TRAILING_PUNCTUATION }
        if (url.isEmpty()) return@forEach
        addLink(
            LinkAnnotation.Url(url = url, styles = TextLinkStyles(style = linkStyle)),
            start = match.range.first,
            end = match.range.first + url.length,
        )
    }
    emailRegex.findAll(text)
        .filterNot { email -> webRanges.any { email.range.first in it.range } }
        .forEach { match ->
            addLink(
                LinkAnnotation.Url(url = "mailto:${match.value}", styles = TextLinkStyles(style = linkStyle)),
                start = match.range.first,
                end = match.range.last + 1,
            )
        }
}
