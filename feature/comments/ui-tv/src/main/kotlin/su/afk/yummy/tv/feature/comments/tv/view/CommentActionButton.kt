package su.afk.yummy.tv.feature.comments.tv.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick

@Composable
internal fun CommentActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    accentColor: Color? = null,
    showLabel: Boolean = true,
) {
    val shape = RoundedCornerShape(8.dp)
    val contentColor = when {
        accentColor != null && selected -> accentColor
        accentColor != null -> accentColor.copy(alpha = 0.72f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val bgColor = when {
        accentColor != null && selected -> accentColor.copy(alpha = 0.20f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    }
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .tvFocusableClick(onClick = { if (enabled) onClick() }, shape = shape)
            .background(bgColor, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label.takeUnless { showLabel },
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
            if (showLabel) {
                Text(
                    text = label,
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
