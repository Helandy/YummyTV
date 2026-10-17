package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Выбор одного варианта рядом иконок, под которым подписано название выбранного (у иконок подписей
 * нет, чтобы ряд влезал в узкую боковую панель).
 */
@Composable
internal fun <T> MobilePlayerIconChoiceRow(
    options: List<T>,
    selected: T,
    icon: (T) -> ImageVector,
    label: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(shape)
                        .background(
                            if (isSelected) colors.primary.copy(alpha = 0.18f) else colors.surfaceVariant.copy(alpha = 0.58f),
                        )
                        .border(1.dp, if (isSelected) colors.primary else Color.Transparent, shape)
                        .clickable(role = Role.RadioButton) { onSelected(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon(option),
                        contentDescription = label(option),
                        tint = if (isSelected) colors.primary else colors.onSurface,
                    )
                }
            }
        }
        Text(
            text = label(selected),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}
