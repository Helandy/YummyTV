package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/** Single-line-level error text (form field, auth status). Renders nothing when [message] is null or blank. */
@Composable
fun InlineError(
    message: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    textAlign: TextAlign? = null,
) {
    if (message.isNullOrBlank()) return
    Text(
        text = message,
        style = style,
        color = MaterialTheme.colorScheme.error,
        textAlign = textAlign,
        modifier = modifier,
    )
}
