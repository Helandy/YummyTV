package su.afk.yummy.tv.feature.player.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.view.player.TV_PLAYER_FOCUS_ANIMATION_DURATION_MS
import su.afk.yummy.tv.feature.player.view.player.tvPlayerFocusScale

@Composable
internal fun TvOverlayButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)

    val colors = MaterialTheme.colorScheme
    val bgColor by animateColorAsState(
        targetValue = when {
            focused -> colors.primary
            primary -> Color.White.copy(alpha = 0.18f)
            else -> Color.Transparent
        },
        animationSpec = tween(TV_PLAYER_FOCUS_ANIMATION_DURATION_MS),
        label = "tvOverlayButtonBackground",
    )
    val textColor by animateColorAsState(
        targetValue = if (focused) colors.onPrimary else Color.White,
        animationSpec = tween(TV_PLAYER_FOCUS_ANIMATION_DURATION_MS),
        label = "tvOverlayButtonContent",
    )
    val borderColor by animateColorAsState(
        targetValue = if (focused) colors.primary else Color.White.copy(alpha = 0.35f),
        animationSpec = tween(TV_PLAYER_FOCUS_ANIMATION_DURATION_MS),
        label = "tvOverlayButtonBorder",
    )

    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = textColor,
        modifier = modifier
            .tvPlayerFocusScale(focused)
            .border(width = 2.dp, color = borderColor, shape = shape)
            .background(bgColor, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    )
}
