package su.afk.yummy.tv.feature.reviews.model

sealed interface ReviewContentBlock {
    val id: Long

    data class Paragraph(
        override val id: Long,
        val text: String,
        val safeHtml: String? = null,
    ) : ReviewContentBlock

    data class Image(
        override val id: Long,
        val url: String,
        val alt: String = "",
    ) : ReviewContentBlock
}
