@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick

private const val FOCUSED_SCALE = 1.015f

@Composable
internal fun AccountAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    hint: String? = null,
    // Кнопки в ряд: без резерва строк хинта соседи разъезжаются по высоте.
    hintMinLines: Int = 1,
    selected: Boolean = false,
    enabled: Boolean = true,
    iconOnly: Boolean = false,
    onDirectionLeft: (() -> Boolean)? = null,
    onDirectionRight: (() -> Boolean)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val containerColor = when {
        focused && enabled -> MaterialTheme.colorScheme.primary
        selected && enabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.025f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
    }
    val contentColor = when {
        focused && enabled -> MaterialTheme.colorScheme.onPrimary
        selected && enabled -> MaterialTheme.colorScheme.primary
        enabled -> MaterialTheme.colorScheme.onBackground
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val hintColor = when {
        focused && enabled -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val actionModifier = modifier
        .let { if (iconOnly) it else it.fillMaxWidth() }
        .let {
            if (enabled) {
                it.tvFocusableClick(
                    onClick = onClick,
                    interactionSource = interactionSource,
                    shape = shape,
                    // Кнопка во всю ширину: крупный скейл выпирает за фон и режет текст.
                    focusedScale = FOCUSED_SCALE,
                    focusedBorderColor = Color.Transparent,
                )
            } else {
                it
            }
        }
        .accountActionKeyEvents(
            onDirectionLeft = onDirectionLeft,
            onDirectionRight = onDirectionRight,
        )
        .clip(shape)
        .background(
            color = containerColor,
            shape = shape,
        )
        .border(
            width = if (focused && enabled) 3.dp else 2.dp,
            color = if (focused && enabled) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
            else Color.Transparent,
            shape = shape,
        )
        .padding(
            horizontal = if (iconOnly) 12.dp else 16.dp,
            vertical = 14.dp,
        )
    if (iconOnly) {
        Box(modifier = actionModifier, contentAlignment = Alignment.Center) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        return
    }
    Row(
        modifier = actionModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = hintColor,
                    minLines = hintMinLines,
                    maxLines = maxOf(2, hintMinLines),
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun Modifier.accountActionKeyEvents(
    onDirectionLeft: (() -> Boolean)?,
    onDirectionRight: (() -> Boolean)?,
): Modifier {
    if (onDirectionLeft == null && onDirectionRight == null) return this
    return onPreviewKeyEvent { event ->
        handleAccountActionKeyEvent(
            event = event,
            onDirectionLeft = onDirectionLeft,
            onDirectionRight = onDirectionRight,
        )
    }.onKeyEvent { event ->
        handleAccountActionKeyEvent(
            event = event,
            onDirectionLeft = onDirectionLeft,
            onDirectionRight = onDirectionRight,
        )
    }
}

private fun handleAccountActionKeyEvent(
    event: KeyEvent,
    onDirectionLeft: (() -> Boolean)?,
    onDirectionRight: (() -> Boolean)?,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    return when (event.key) {
        Key.DirectionLeft -> onDirectionLeft?.invoke() ?: false
        Key.DirectionRight -> onDirectionRight?.invoke() ?: false
        else -> false
    }
}
