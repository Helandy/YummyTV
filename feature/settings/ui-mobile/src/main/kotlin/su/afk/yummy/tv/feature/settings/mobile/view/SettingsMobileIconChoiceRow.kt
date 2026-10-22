package su.afk.yummy.tv.feature.settings.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Один вариант переключателя [SettingsMobileIconChoiceRow]: значок и его подпись для доступности. */
internal data class SettingsMobileIconChoice<T>(
    val value: T,
    val icon: ImageVector,
    val description: String,
)

/** Строка настройки, где варианты выбираются значками прямо в строке, без шторки. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> SettingsMobileIconChoiceRow(
    label: String,
    hint: String,
    choices: List<SettingsMobileIconChoice<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primary,
        activeContentColor = MaterialTheme.colorScheme.onPrimary,
        activeBorderColor = MaterialTheme.colorScheme.primary,
        inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (hint.isNotBlank()) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.width((choices.size * 52).dp)) {
            choices.forEachIndexed { index, choice ->
                SegmentedButton(
                    selected = choice.value == selected,
                    onClick = { onSelected(choice.value) },
                    shape = SegmentedButtonDefaults.itemShape(index, choices.size),
                    colors = colors,
                    icon = {},
                    label = {
                        Icon(
                            imageVector = choice.icon,
                            contentDescription = choice.description,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
            }
        }
    }
}
