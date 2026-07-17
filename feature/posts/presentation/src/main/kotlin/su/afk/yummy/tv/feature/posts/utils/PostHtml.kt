package su.afk.yummy.tv.feature.posts.utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist
import su.afk.yummy.tv.core.utils.toHttpsUrl
import su.afk.yummy.tv.feature.posts.model.PostContentBlock

private val postSafeList = Safelist.none()
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
        "h4",
        "ul",
        "ol",
        "li",
        "blockquote"
    )
    .addAttributes("a", "href")
    .addAttributes("img", "src", "alt")
    .addProtocols("a", "href", "https")
    .addProtocols("img", "src", "https")

fun String.parsePostContent(excludedImageUrl: String? = null): List<PostContentBlock> {
    val normalizedExcludedImageUrl = excludedImageUrl?.toHttpsUrl()
    val normalizedHtml = Jsoup.parseBodyFragment(this).apply {
        select("img[src]").forEach { image ->
            image.attr("src", image.attr("src").toHttpsUrl())
        }
    }.body().html()
    val safeHtml = Jsoup.clean(normalizedHtml, "", postSafeList)
    val body = Jsoup.parseBodyFragment(safeHtml).body()
    val result = mutableListOf<PostContentBlock>()

    fun add(element: Element) {
        if (element.tagName() == "img") {
            element.attr("src").takeIf {
                it.startsWith("https://") && it != normalizedExcludedImageUrl
            }?.let {
                result += PostContentBlock.Image(it, element.attr("alt"))
            }
            return
        }
        val images = element.select("img").toList()
        val text = element.clone().also { it.select("img").remove() }.apply {
            select("br").append("\n")
            select("li").prepend("• ").append("\n")
        }.wholeText().replace(Regex("\\n{3,}"), "\n\n").trim()
        if (text.isNotBlank()) result += PostContentBlock.Text(text)
        images.forEach(::add)
    }

    body.children().forEach(::add)
    return result
}
