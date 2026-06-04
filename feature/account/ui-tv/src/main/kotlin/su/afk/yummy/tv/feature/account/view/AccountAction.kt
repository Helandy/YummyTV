@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick

@Composable
internal fun AccountAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val active = enabled && (focused || selected)
    val actionModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .border(
            width = 2.dp,
            color = if (focused && enabled) MaterialTheme.colorScheme.primary else Color.Transparent,
            shape = shape,
        )
        .background(
            color = when {
                focused && enabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                selected && enabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.025f)
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
            },
            shape = shape,
        )
        .let {
            if (enabled) {
                it.tvFocusableClick(onClick = onClick, interactionSource = interactionSource, shape = shape)
            } else {
                it
            }
        }
        .padding(horizontal = 16.dp, vertical = 14.dp)
    Row(
        modifier = actionModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    active -> MaterialTheme.colorScheme.primary
                    enabled -> MaterialTheme.colorScheme.onBackground
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
