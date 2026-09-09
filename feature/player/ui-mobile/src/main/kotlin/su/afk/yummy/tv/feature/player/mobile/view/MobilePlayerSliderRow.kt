package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Половина ширины ползунка Material3: на столько трек вставлен внутрь с каждой стороны. */
private val SliderThumbInset = 10.dp

/** Слайдер для выбора качества/скорости в нижнем баре настроек плеера, тот же стиль, что в Настройках. */
@Composable
internal fun MobilePlayerSliderRow(
    valueText: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tickLabels: List<String>? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = (valueRange.last - valueRange.first - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
        if (tickLabels != null && tickLabels.size > 1) {
            MobilePlayerSliderTickLabels(
                labels = tickLabels,
                selectedIndex = value - valueRange.first,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

/**
 * Подписи делений под треком: каждая центрируется по своему делению.
 * Row(SpaceBetween) не годится — трек вставлен внутрь на радиус ползунка, и края бы разъехались.
 */
@Composable
private fun MobilePlayerSliderTickLabels(
    labels: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    Layout(
        modifier = modifier,
        content = {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) selectedColor else unselectedColor,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints()) }
        val width = constraints.maxWidth
        val height = placeables.maxOfOrNull { it.height } ?: 0
        val inset = SliderThumbInset.roundToPx()
        val trackWidth = (width - inset * 2).coerceAtLeast(0)
        val lastIndex = (placeables.size - 1).coerceAtLeast(1)
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val center = inset + trackWidth * index / lastIndex
                val x = (center - placeable.width / 2).coerceIn(
                    0,
                    (width - placeable.width).coerceAtLeast(0),
                )
                placeable.placeRelative(x = x, y = (height - placeable.height) / 2)
            }
        }
    }
}
