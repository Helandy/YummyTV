package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.mobile.account.utils.linkifyUrls
import su.afk.yummy.tv.feature.account.presentation.R

/**
 * «О себе»: свёрнуто до [collapsedLines] строк, раскрывается тапом или шевроном справа —
 * как описание серии; ссылки и почта кликабельны.
 */
@Composable
internal fun AccountMobileProfileAbout(
    text: String,
    modifier: Modifier = Modifier,
    collapsedLines: Int = 4,
) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    // Переполнение защёлкиваем: в раскрытом виде maxLines не ограничен и hasVisualOverflow всегда false.
    var canExpand by remember(text) { mutableStateOf(false) }
    val toggleable = canExpand || expanded
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, linkColor) {
        text.linkifyUrls(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            // Скругляем, чтобы подсветка нажатия не была квадратной.
            .clip(RoundedCornerShape(12.dp))
            .then(if (toggleable) Modifier.clickable { expanded = !expanded } else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded && it.hasVisualOverflow) canExpand = true },
            modifier = Modifier.weight(1f),
        )
        if (toggleable) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = stringResource(
                    if (expanded) R.string.account_mobile_about_collapse else R.string.account_mobile_about_expand,
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
