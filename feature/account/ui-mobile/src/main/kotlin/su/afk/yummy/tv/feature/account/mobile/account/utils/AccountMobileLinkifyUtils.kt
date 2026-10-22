package su.afk.yummy.tv.feature.account.mobile.account.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import su.afk.yummy.tv.core.designsystem.utils.addUrlLinks

/** Текст, где http(s)-ссылки и почтовые адреса кликабельны. */
internal fun String.linkifyUrls(linkStyle: SpanStyle): AnnotatedString = buildAnnotatedString {
    append(this@linkifyUrls)
    addUrlLinks(this@linkifyUrls, linkStyle)
}
