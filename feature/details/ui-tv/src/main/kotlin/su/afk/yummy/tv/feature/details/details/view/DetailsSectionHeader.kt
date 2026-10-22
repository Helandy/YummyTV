package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick

/** Заголовок секции ленты деталей; [actionLabel] — фокусируемая кнопка «Показать все» справа. */
@Composable
internal fun DetailsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: () -> Unit = {},
    /** Если задан, кнопка берёт фокус только по явному запросу, а не от стрелок вверх/вниз. */
    actionFocus: HeaderActionFocus? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (actionLabel != null) {
            val shape = RoundedCornerShape(8.dp)
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .then(if (actionFocus != null) Modifier.focusRequester(actionFocus.requester) else Modifier)
                    .focusProperties { canFocus = actionFocus?.reachable ?: true }
                    .onFocusChanged { actionFocus?.onFocusChanged(it.isFocused) }
                    .tvFocusableClick(onClick = onActionClick, shape = shape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

internal val DetailsSectionHorizontalPadding = 20.dp
