package su.afk.yummy.tv.feature.catalog.view

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

private const val SORT_DIRECTION_ANIMATION_MILLIS = 180

/**
 * Row of a sort option. The selected one shows the order with a rotating arrow instead of
 * the selection mark, and a click on it reverses the order. The arrow points down for the
 * option's natural order (best first) and up when it is [reversed]; `null` for an unselected
 * option, so no arrow is shown.
 */
@Composable
internal fun SortRow(
    label: String,
    selected: Boolean,
    reversed: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (reversed == true) 0f else 180f,
        animationSpec = tween(durationMillis = SORT_DIRECTION_ANIMATION_MILLIS),
        label = "sortDirectionRotation",
    )
    SelectableRow(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        trailingIcon = if (selected && reversed != null) {
            {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(18.dp)
                        .rotate(rotation),
                )
            }
        } else {
            null
        },
    )
}
