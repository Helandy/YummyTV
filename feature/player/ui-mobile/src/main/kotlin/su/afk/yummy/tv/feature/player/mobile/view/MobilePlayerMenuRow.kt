package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Режим правой части строки меню настроек. */
internal sealed interface MobilePlayerMenuRowTrailing {
    /** Значение и стрелка «вглубь». */
    data class Navigate(val value: String? = null) : MobilePlayerMenuRowTrailing

    /** Переключатель; клик по всей строке меняет его. */
    data class Toggle(val checked: Boolean) : MobilePlayerMenuRowTrailing

    /** Галочка у выбранного варианта списка. */
    data class Choice(val selected: Boolean) : MobilePlayerMenuRowTrailing

    data object None : MobilePlayerMenuRowTrailing
}

/** Строка единого меню настроек в стиле YouTube: иконка, название, значение/переключатель справа. */
@Composable
internal fun MobilePlayerMenuRow(
    label: String,
    trailing: MobilePlayerMenuRowTrailing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    /** Поясняющая строка под названием (мельче и приглушённее). */
    subtitle: String? = null,
    /** Цвет названия; по умолчанию обычный цвет текста. */
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(
                role = if (trailing is MobilePlayerMenuRowTrailing.Toggle) Role.Switch else Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Колонка иконки есть у всех строк, чтобы заголовки стояли в одну линию (и в списках выбора).
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            when {
                icon != null -> Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                trailing is MobilePlayerMenuRowTrailing.Choice && trailing.selected -> Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (trailing is MobilePlayerMenuRowTrailing.Choice && trailing.selected) {
                    FontWeight.SemiBold
                } else {
                    null
                },
                color = labelColor,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when (trailing) {
            is MobilePlayerMenuRowTrailing.Navigate -> {
                if (trailing.value != null) {
                    Text(
                        text = trailing.value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 180.dp),
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is MobilePlayerMenuRowTrailing.Toggle -> Switch(checked = trailing.checked, onCheckedChange = null)
            is MobilePlayerMenuRowTrailing.Choice, MobilePlayerMenuRowTrailing.None -> Unit
        }
    }
}
