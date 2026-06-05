package su.afk.yummy.tv.feature.player.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
internal fun MobilePlayerActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)
    val background = when {
        !enabled -> Color.White.copy(alpha = 0.06f)
        primary -> Color.White.copy(alpha = 0.92f)
        else -> Color.White.copy(alpha = 0.14f)
    }
    val contentColor = when {
        !enabled -> Color.White.copy(alpha = 0.26f)
        primary -> Color.Black
        else -> Color.White
    }
    val borderColor =
        if (primary) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.18f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(if (primary) 56.dp else 46.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor)
    }
}
