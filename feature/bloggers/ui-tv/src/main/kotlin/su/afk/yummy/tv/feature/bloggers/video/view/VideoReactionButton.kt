package su.afk.yummy.tv.feature.bloggers.video.view

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.utils.toCompactCount

/** Кнопка реакции (лайк/дизлайк) с цветным контентом и видимым фокусом. */
@Composable
internal fun VideoReactionButton(
    icon: ImageVector,
    count: Int,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val bgColor =
        if (selected) color.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .tvFocusableClick(onClick = onClick, shape = shape)
            .background(bgColor, shape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(count.toCompactCount(), color = color, style = MaterialTheme.typography.labelLarge)
        }
    }
}

