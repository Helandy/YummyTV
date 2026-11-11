package su.afk.yummy.tv.core.designsystem.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

private val GoodColor = Color(0xFF66BB6A)
private val WarningColor = Color(0xFFFFB74D)
private val BadColor = Color(0xFFEF5350)

/** Цвет значения по оттенку; [neutral] — для обычных значений (зависит от фона карточки). */
internal fun ChoiceDialogTone.color(neutral: Color): Color = when (this) {
    ChoiceDialogTone.Neutral -> neutral
    ChoiceDialogTone.Good -> GoodColor
    ChoiceDialogTone.Warning -> WarningColor
    ChoiceDialogTone.Bad -> BadColor
}

/**
 * Чипы-метки карточки. Первая метка (обычно качество) выделена акцентом, остальные — спокойным фоном.
 * [chipColor]/[onChipColor] — цвет обычных чипов и текста на них, чтобы карточка выглядела на светлом
 * (сфокусированная строка ТВ) и тёмном фоне.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChoiceBadges(
    badges: List<String>,
    markers: List<String> = emptyList(),
    warnings: List<String> = emptyList(),
    chipColor: Color,
    onChipColor: Color,
    accentColor: Color,
    onAccentColor: Color,
    modifier: Modifier = Modifier,
) {
    if (badges.isEmpty() && markers.isEmpty() && warnings.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Метки статуса: сплошная зелёная заливка с галочкой, чтобы бросались в глаза на фоне обычных чипов.
        markers.forEach { marker ->
            Text(
                text = "✓ " + marker,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(alpha = 0.85f),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(GoodColor)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
        warnings.forEach { warning ->
            Text(
                text = "⚠ " + warning,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(alpha = 0.85f),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(WarningColor)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
        badges.forEachIndexed { index, badge ->
            val accent = index == 0
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (accent) onAccentColor else onChipColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (accent) accentColor else chipColor)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
    }
}

/** Строка сведений: значения через « · », каждое окрашено по [ChoiceDialogStat.tone]. */
@Composable
internal fun ChoiceStats(
    stats: List<ChoiceDialogStat>,
    neutralColor: Color,
    modifier: Modifier = Modifier,
) {
    if (stats.isEmpty()) return
    Text(
        text = buildAnnotatedString {
            stats.forEachIndexed { index, stat ->
                if (index > 0) withStyle(SpanStyle(color = neutralColor)) { append("  ·  ") }
                withStyle(SpanStyle(color = stat.tone.color(neutralColor), fontWeight = FontWeight.Medium)) {
                    append(stat.text)
                }
            }
        },
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
    )
}
