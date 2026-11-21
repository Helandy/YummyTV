package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import su.afk.yummy.tv.feature.account.presentation.R

private const val YANI_SITE_URL = "https://ru.yummyani.me"

@Composable
internal fun RegistrationTermsNotice() {
    val linkText = stringResource(R.string.account_registration_terms_link_text)
    val notice = stringResource(R.string.account_registration_terms_notice, linkText)
    val linkStart = notice.indexOf(linkText)
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
    )
    val annotated = buildAnnotatedString {
        if (linkStart < 0) {
            append(notice)
            return@buildAnnotatedString
        }
        append(notice.substring(0, linkStart))
        withLink(
            LinkAnnotation.Url(url = YANI_SITE_URL, styles = linkStyles),
        ) {
            append(linkText)
        }
        append(notice.substring(linkStart + linkText.length))
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
