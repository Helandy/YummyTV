package su.afk.yummy.tv.feature.reviews.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.safety.Safelist
import su.afk.yummy.tv.feature.reviews.model.ReviewContentBlock

private val safeList = Safelist.none()
    .addTags(
        "div",
        "p",
        "br",
        "b",
        "strong",
        "i",
        "em",
        "a",
        "img",
        "h1",
        "h2",
        "h3",
        "ul",
        "ol",
        "li",
        "blockquote"
    )
    .addAttributes("a", "href")
    .addAttributes("img", "src", "alt")
    .addProtocols("a", "href", "https")
    .addProtocols("img", "src", "https")

fun sanitizeReviewHtml(html: String): String = Jsoup.clean(html, "", safeList)

fun parseReviewBlocks(html: String): List<ReviewContentBlock> {
    val body = Jsoup.parseBodyFragment(sanitizeReviewHtml(html)).body()
    var id = 0L
    val result = mutableListOf<ReviewContentBlock>()
    fun add(element: Element) {
        if (element.tagName() == "img") {
            element.attr("src").takeIf { it.startsWith("https://") }?.let {
                result += ReviewContentBlock.Image(++id, it, element.attr("alt"))
            }
            return
        }
        val ownImages = element.select("img").toList()
        val text = element.clone().also { it.select("img").remove() }.text()
        if (text.isNotBlank()) {
            result += ReviewContentBlock.Paragraph(
                id = ++id,
                text = text,
                safeHtml = element.outerHtml(),
            )
        }
        ownImages.forEach(::add)
    }
    body.children().forEach(::add)
    return result.ifEmpty { listOf(ReviewContentBlock.Paragraph(1, "")) }
}

fun reviewAnnotatedText(block: ReviewContentBlock.Paragraph): AnnotatedString {
    val safeHtml = block.safeHtml ?: block.text
    val root = Jsoup.parseBodyFragment(sanitizeReviewHtml(safeHtml)).body()
    return buildAnnotatedString {
        fun appendNode(node: Node) {
            when (node) {
                is TextNode -> append(node.text())
                is Element -> when (node.tagName()) {
                    "br" -> append('\n')
                    "img" -> Unit
                    "b", "strong" -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        node.childNodes().forEach(::appendNode)
                    }

                    "i", "em" -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        node.childNodes().forEach(::appendNode)
                    }

                    "h1", "h2", "h3" -> withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    ) { node.childNodes().forEach(::appendNode) }

                    "a" -> {
                        val url = node.attr("href").takeIf { it.startsWith("https://") }
                        if (url != null) {
                            withLink(
                                LinkAnnotation.Url(
                                    url = url,
                                    styles = TextLinkStyles(
                                        style = SpanStyle(textDecoration = TextDecoration.Underline),
                                    ),
                                ),
                            ) {
                                node.childNodes().forEach(::appendNode)
                            }
                        } else node.childNodes().forEach(::appendNode)
                    }

                    "li" -> {
                        append("• ")
                        node.childNodes().forEach(::appendNode)
                        append('\n')
                    }

                    else -> node.childNodes().forEach(::appendNode)
                }
            }
        }
        root.childNodes().forEach(::appendNode)
    }
}
