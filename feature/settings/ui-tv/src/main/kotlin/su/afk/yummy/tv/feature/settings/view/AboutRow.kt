package su.afk.yummy.tv.feature.settings.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun AboutRow(
    label: String,
    hint: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 2.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape,
            )
            .background(
                color = if (focused) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Transparent,
                shape = shape,
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = if (onClick != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
