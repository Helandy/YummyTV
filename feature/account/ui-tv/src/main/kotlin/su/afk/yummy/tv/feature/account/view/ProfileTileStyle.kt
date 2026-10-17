package su.afk.yummy.tv.feature.account.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import su.afk.yummy.tv.feature.account.common.utils.label

private const val TILE_ANIMATION_DURATION_MS = 150

/**
 * Общий вид плитки профиля: анимированные фон, рамка и лёгкое увеличение при фокусе.
 * Ставится в цепочке после focus-модификаторов и до внутреннего padding.
 */
@Composable
internal fun Modifier.profileTileVisual(
    focused: Boolean,
    shape: Shape = RoundedCornerShape(12.dp),
    focusedScale: Float = 1.02f,
    unfocusedContainerAlpha: Float = 0.06f,
): Modifier {
    val containerColor by animateColorAsState(
        targetValue = if (focused) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = unfocusedContainerAlpha)
        },
        animationSpec = tween(TILE_ANIMATION_DURATION_MS),
        label = "profileTileContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(TILE_ANIMATION_DURATION_MS),
        label = "profileTileBorder",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (focused) 3.dp else 2.dp,
        animationSpec = tween(TILE_ANIMATION_DURATION_MS),
        label = "profileTileBorderWidth",
    )
    val scale by animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        animationSpec = tween(TILE_ANIMATION_DURATION_MS),
        label = "profileTileScale",
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
        .background(containerColor)
        .border(width = borderWidth, color = borderColor, shape = shape)
}

@Composable
internal fun ProfileSectionHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 0.6.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
