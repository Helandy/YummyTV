package su.afk.yummy.tv.feature.account.mobile.account.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import su.afk.yummy.tv.domain.account.model.NotificationLink

/**
 * Текст уведомления, где названия аниме из [links] — кликабельные ссылки. Название ищется в тексте
 * последовательно, поэтому ссылка, которой в тексте нет (например, только в заголовке), пропускается.
 */
internal fun buildNotificationText(
    text: String,
    links: List<NotificationLink>,
    linkStyle: SpanStyle,
    onLinkClick: (slug: String) -> Unit,
): AnnotatedString = buildAnnotatedString {
    append(text)
    var cursor = 0
    links.forEach { link ->
        val start = text.indexOf(link.label, startIndex = cursor)
        if (start < 0) return@forEach
        val end = start + link.label.length
        addLink(
            LinkAnnotation.Clickable(
                tag = link.slug,
                styles = TextLinkStyles(style = linkStyle),
                linkInteractionListener = { onLinkClick(link.slug) },
            ),
            start = start,
            end = end,
        )
        cursor = end
    }
}
