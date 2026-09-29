package su.afk.yummy.tv.feature.settings.mobile.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Слайдер настройки. Во время перетаскивания меняются только ползунок и подпись;
 * [onValueCommitted] вызывается один раз, когда палец отпущен. Иначе Slider шлёт значение на
 * каждый кадр, и каждое уходило бы в DataStore, в аналитику и в пересборку всего экрана.
 */
@Composable
internal fun SettingsMobileSliderRow(
    label: String,
    valueLabel: @Composable (Int) -> String,
    value: Int,
    valueRange: IntRange,
    enabled: Boolean,
    onValueCommitted: (Int) -> Unit,
    modifier: Modifier = Modifier,
    stepSize: Int = 1,
) {
    var dragValue by remember(value) { mutableStateOf<Int?>(null) }
    val shownValue = dragValue ?: value
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (enabled) FontWeight.SemiBold else FontWeight.Normal,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueLabel(shownValue),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = shownValue.toFloat(),
            onValueChange = { dragValue = it.roundToInt() },
            onValueChangeFinished = {
                val committed = dragValue ?: return@Slider
                dragValue = null
                if (committed != value) onValueCommitted(committed)
            },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = ((valueRange.last - valueRange.first) / stepSize - 1).coerceAtLeast(0),
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}
