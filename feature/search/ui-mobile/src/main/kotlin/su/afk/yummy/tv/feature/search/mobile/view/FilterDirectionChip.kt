package su.afk.yummy.tv.feature.search.mobile.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

/** Chip that toggles sort direction and shows it with a rotating arrow. */
@Composable
internal fun FilterDirectionChip(
    label: String,
    forward: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (forward) 0f else 180f,
        animationSpec = tween(durationMillis = CHIP_ANIMATION_MILLIS),
        label = "sortDirectionRotation",
    )
    FilterChip(
        label = label,
        selected = false,
        onClick = onClick,
        modifier = modifier,
        trailingIcon = {
            Icon(
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(start = 5.dp)
                    .size(15.dp)
                    .rotate(rotation),
            )
        },
    )
}
