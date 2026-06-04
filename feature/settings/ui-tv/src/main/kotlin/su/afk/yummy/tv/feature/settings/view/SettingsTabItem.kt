package su.afk.yummy.tv.feature.settings.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsTabItem(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    contentFocusRequester: FocusRequester,
    onSelected: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    val contentColor = when {
        focused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.onBackground
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val indicatorColor = when {
        focused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        else -> Color.Transparent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .focusProperties {
                down = contentFocusRequester
            }
            .onFocusChanged { if (it.isFocused) onSelected() }
            .background(
                color = if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(interactionSource = interactionSource, indication = null) { onSelected() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(2.dp)
                .background(color = indicatorColor, shape = RoundedCornerShape(1.dp)),
        )
    }
}
