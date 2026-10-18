package su.afk.yummy.tv.feature.account.mobile.account.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString

private val webUrlRegex = Regex("""https?://[^\s<>"']+""")
private val emailRegex = Regex("""[\w.+-]+@[\w-]+(\.[\w-]+)+""")
private const val TRAILING_PUNCTUATION = ".,;:!?)»"

/**
 * Текст, где http(s)-ссылки и почтовые адреса кликабельны ([LinkAnnotation.Url], почта — через mailto).
 * Знаки препинания в конце ссылки («…/support.») в неё не входят.
 */
internal fun String.linkifyUrls(linkStyle: SpanStyle): AnnotatedString = buildAnnotatedString {
    append(this@linkifyUrls)
    webUrlRegex.findAll(this@linkifyUrls).forEach { match ->
        val url = match.value.trimEnd { it in TRAILING_PUNCTUATION }
        if (url.isEmpty()) return@forEach
        addLink(
            LinkAnnotation.Url(url = url, styles = TextLinkStyles(style = linkStyle)),
            start = match.range.first,
            end = match.range.first + url.length,
        )
    }
    emailRegex.findAll(this@linkifyUrls)
        .filterNot { email -> webUrlRegex.findAll(this@linkifyUrls).any { email.range.first in it.range } }
        .forEach { match ->
            addLink(
                LinkAnnotation.Url(url = "mailto:${match.value}", styles = TextLinkStyles(style = linkStyle)),
                start = match.range.first,
                end = match.range.last + 1,
            )
        }
}
