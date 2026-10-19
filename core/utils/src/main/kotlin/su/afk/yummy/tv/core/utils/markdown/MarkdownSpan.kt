package su.afk.yummy.tv.core.utils.markdown

/** Фрагмент строки markdown с единым оформлением; [link] — адрес, если фрагмент — ссылка. */
data class MarkdownSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val strike: Boolean = false,
    val code: Boolean = false,
    val link: String? = null,
)
