package su.afk.yummy.tv.feature.settings.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
internal fun FocusableIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
        focused -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val backgroundColor = when {
        !enabled -> Color.Transparent
        focused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(shape)
            .border(
                width = 2.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape,
            )
            .background(backgroundColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (enabled) onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
        )
    }
}
