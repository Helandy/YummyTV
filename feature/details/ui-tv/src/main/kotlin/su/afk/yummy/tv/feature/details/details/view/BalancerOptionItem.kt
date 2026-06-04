package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.details.R

@Composable
internal fun BalancerOptionItem(
    label: String,
    focusRequester: FocusRequester?,
    isSupported: Boolean = true,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    if (isSupported) {
        val bgColor by animateColorAsState(
            targetValue = if (focused) Color.White else Color.White.copy(alpha = 0.12f),
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
            label = "balancer_bg",
        )
        val textColor by animateColorAsState(
            targetValue = if (focused) Color.Black else Color.White,
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
            label = "balancer_text",
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            modifier = Modifier
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .clip(shape)
                .background(bgColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 24.dp, vertical = 14.dp),
        )
    } else {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .clip(shape)
                .background(Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.35f),
            )
            Text(
                text = stringResource(R.string.details_unsupported),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.25f),
            )
        }
    }
}
