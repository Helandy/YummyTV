package su.afk.yummy.tv.feature.details.full.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.feature.details.full.model.LocalFocusedChipReporter

@Composable
internal fun FullDetailsChip(
    label: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    val clickable = onClick != null
    val primary = MaterialTheme.colorScheme.primary
    val reporter = LocalFocusedChipReporter.current
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = if (clickable) primary else MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .onGloballyPositioned { coordinates = it }
            .onFocusChanged { state -> if (state.isFocused) coordinates?.let { reporter?.invoke(it) } }
            .background(
                color = if (clickable) {
                    primary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                shape = shape,
            )
            .then(
                if (onClick != null) {
                    Modifier
                        .border(
                            border = BorderStroke(1.dp, primary.copy(alpha = 0.5f)),
                            shape = shape,
                        )
                        .tvFocusableClick(onClick = onClick, shape = shape)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}
