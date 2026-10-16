package su.afk.yummy.tv.feature.catalog.mobile.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

/**
 * Chip of a sort option. The selected one shows the order with a rotating arrow instead of
 * the selection checkmark, and a tap on it reverses the order. The arrow points down for the
 * option's natural order (best first) and up when it is [reversed]; `null` for an unselected
 * option, so no arrow is shown.
 */
@Composable
internal fun SortChip(
    label: String,
    selected: Boolean,
    reversed: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (reversed == true) 0f else 180f,
        animationSpec = tween(durationMillis = CHIP_ANIMATION_MILLIS),
        label = "sortDirectionRotation",
    )
    FilterChip(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        showSelectedIcon = reversed == null,
        trailingIcon = if (selected && reversed != null) {
            {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .size(15.dp)
                        .rotate(rotation),
                )
            }
        } else {
            null
        },
    )
}
